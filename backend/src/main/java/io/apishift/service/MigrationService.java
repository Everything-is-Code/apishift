package io.apishift.service;

import io.apishift.ai.MigrationAgent;
import io.apishift.model.MigrationPlan;
import io.apishift.model.MigrationPrerequisite;
import io.apishift.model.TestCommand;
import io.apishift.model.ThreeScaleProduct;
import io.apishift.model.AuditEntry;
import io.apishift.repository.AuditRepository;
import io.apishift.repository.MigrationPlanRepository;
import io.apishift.service.generator.AuthPolicyResourceGenerator;
import io.apishift.service.generator.GatewayResourceGenerator;
import io.apishift.service.generator.HttpRouteResourceGenerator;
import io.apishift.service.generator.PlanPolicyResourceGenerator;
import io.apishift.service.generator.RateLimitResourceGenerator;
import io.apishift.service.generator.ResolvedBackend;
import io.apishift.service.migration.BackendEndpointResolver;
import io.apishift.service.migration.OpenApiSynthesisService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class MigrationService {

    private static final Logger LOG = Logger.getLogger(MigrationService.class);

    @Inject
    ThreeScaleService threeScaleService;

    @Inject
    ClusterRegistry clusterRegistry;

    @Inject
    KuadrantCtlService kuadrantCtlService;

    @Inject
    MigrationAgent migrationAgent;

    @Inject
    ApiShiftMetrics metrics;

    @Inject
    PrerequisiteCatalogService prerequisiteCatalogService;

    @Inject
    ToolConfigPrerequisiteChecker toolConfigPrerequisiteChecker;

    @Inject
    ClusterReadinessService clusterReadinessService;

    @Inject
    MigrationPlanRepository migrationPlanRepository;

    @Inject
    AuditRepository auditRepository;

    @Inject
    GatewayResourceGenerator gatewayResourceGenerator;

    @Inject
    HttpRouteResourceGenerator httpRouteResourceGenerator;

    @Inject
    AuthPolicyResourceGenerator authPolicyResourceGenerator;

    @Inject
    RateLimitResourceGenerator rateLimitResourceGenerator;

    @Inject
    PlanPolicyResourceGenerator planPolicyResourceGenerator;

    @Inject
    BackendEndpointResolver backendEndpointResolver;

    @Inject
    OpenApiSynthesisService openApiSynthesisService;

    @ConfigProperty(name = "apishift.connectivity-link.gateway-class-name", defaultValue = "istio")
    String gatewayClassName;

    @ConfigProperty(name = "apishift.connectivity-link.target-namespace", defaultValue = "kuadrant-system")
    String gatewayNamespace;

    @ConfigProperty(name = "apishift.cluster-domain", defaultValue = "apps.cluster.example.com")
    String clusterDomain;

    @ConfigProperty(name = "apishift.developer-hub.enabled", defaultValue = "false")
    boolean developerHubEnabled;

    @ConfigProperty(name = "apishift.developer-hub.url", defaultValue = "none")
    String developerHubUrl;

    @ConfigProperty(name = "apishift.developer-hub.component-suffix", defaultValue = "-product")
    String componentSuffix;

    @ConfigProperty(name = "apishift.observability.enabled", defaultValue = "false")
    boolean observabilityEnabled;

    private record ProductContext(
            String oasContent, String backendSvcName, String namespace,
            List<ThreeScaleProduct.MappingRule> effectiveRules, boolean hasRealOas,
            List<ResolvedBackend> resolvedBackends) {

        ProductContext(String oasContent, String backendSvcName, String namespace,
                       List<ThreeScaleProduct.MappingRule> effectiveRules, boolean hasRealOas) {
            this(oasContent, backendSvcName, namespace, effectiveRules, hasRealOas, List.of());
        }
    }

    public MigrationPlan analyze(String gatewayStrategy, List<String> productNames, String targetClusterId) {
        io.micrometer.core.instrument.Timer.Sample timerSample = metrics.startMigrationTimer();
        List<String> consolidationWarnings = new ArrayList<>();
        List<ThreeScaleProduct> products = threeScaleService.listProducts().stream()
                .filter(p -> productNames.isEmpty() || productNames.contains(p.name()))
                .toList();
        metrics.setProductsDiscovered(products.size());

        BackendEndpointResolver.BackendIndex backendEndpoints = backendEndpointResolver.resolveIndex();

        List<MigrationPlan.GeneratedResource> resources = new ArrayList<>();
        Map<String, String> oasCache = new LinkedHashMap<>();
        String gatewayName;

        if ("dual".equals(gatewayStrategy)) {
            resources.add(gatewayResourceGenerator.build("apishift-internal", "internal"));
            resources.add(gatewayResourceGenerator.build("apishift-external", "external"));
            gatewayName = "apishift-external";
        } else if ("dedicated".equals(gatewayStrategy)) {
            gatewayName = null;
        } else {
            resources.add(gatewayResourceGenerator.build("apishift-shared", "shared"));
            gatewayName = "apishift-shared";
        }

        for (ThreeScaleProduct product : products) {
            String sysName = product.systemName();
            String routeName = sysName + "-route";

            if ("dedicated".equals(gatewayStrategy)) {
                String gwName = sysName + "-gw";
                resources.add(gatewayResourceGenerator.build(gwName, sysName));
                gatewayName = gwName;
            }

            ProductContext ctx = resolveProductContext(product, backendEndpoints);

            String ns = ctx.namespace;
            if (ns == null || ns.isBlank()) ns = product.backendNamespace();
            if (ns == null || ns.isBlank()) ns = gatewayNamespace;

            String hostname = sysName + "." + clusterDomain;
            String effectiveOas = ctx.oasContent;

            if (effectiveOas == null) {
                effectiveOas = openApiSynthesisService.synthesizeFromMappingRules(product, ctx.effectiveRules, hostname);
            }

            oasCache.put(sysName, effectiveOas);

            boolean usedKuadrantctl = false;
            boolean multiBackend = ctx.resolvedBackends.size() > 1;

            if (!multiBackend && effectiveOas != null) {
                try {
                    String httpRouteOut = kuadrantCtlService.generateHttpRoute(effectiveOas);
                    if (httpRouteOut != null && !httpRouteOut.startsWith("ERROR") && httpRouteOut.contains("kind")) {
                        String patchedHttp = patchKuadrantctlOutput(httpRouteOut, "HTTPRoute", routeName,
                                ns, gatewayName, gatewayNamespace, sysName, hostname);
                        patchedHttp = consolidateHttpRouteRules(patchedHttp, ctx.backendSvcName, consolidationWarnings);
                        resources.add(new MigrationPlan.GeneratedResource("HTTPRoute", routeName, ns, patchedHttp));
                        usedKuadrantctl = true;
                        LOG.infof("kuadrantctl generated HTTPRoute for %s:\n%s", sysName, patchedHttp);
                    }
                } catch (Exception e) {
                    LOG.warnf("kuadrantctl HTTPRoute failed for %s: %s", sysName, e.getMessage());
                }
            } else if (multiBackend) {
                LOG.infof("Product %s has %d backends, skipping kuadrantctl for multi-backend HTTPRoute",
                        sysName, ctx.resolvedBackends.size());
            }

            if (!usedKuadrantctl) {
                resources.add(httpRouteResourceGenerator.build(routeName, ns, gatewayName, product, ctx.effectiveRules,
                        ctx.backendSvcName, ctx.resolvedBackends));
            }

            ThreeScaleProduct productReady = threeScaleService.refreshProductForMigration(product);
            ThreeScaleAuthMode authMode = ThreeScaleAuthMode.fromProduct(productReady);

            if (developerHubEnabled) {
                resources.add(buildApiProduct(sysName, ns, routeName, productReady));
            } else {
                consolidationWarnings.add(
                        "Product '%s': APIProduct skipped — enable ApiShift.developer-hub.enabled for Developer Portal catalog tier."
                                .formatted(sysName));
            }

            RateLimitResourceGenerator.DerivedRateLimit derivedRateLimit =
                    RateLimitResourceGenerator.deriveGlobalRateLimit(productReady);
            if (derivedRateLimit.placeholder()) {
                consolidationWarnings.add(
                        "Product '%s': RateLimitPolicy uses placeholder 100 req/60s — no application plan minute/hour limits found in 3scale."
                                .formatted(sysName));
            }

            if (productReady.applicationPlans() != null && !productReady.applicationPlans().isEmpty()) {
                resources.add(planPolicyResourceGenerator.build(sysName + "-plans", ns, routeName, productReady, authMode));
            }

            if (authMode == ThreeScaleAuthMode.API_KEY) {
                List<MigrationPlan.GeneratedResource> apiKeySecrets =
                        buildConsumerApiKeySecrets(sysName, ns, productReady, consolidationWarnings);
                if (!apiKeySecrets.isEmpty()) {
                    resources.addAll(apiKeySecrets);
                    int appCount = productReady.applications() != null ? productReady.applications().size() : 0;
                    consolidationWarnings.add(
                            "Product '%s' (API Key): %d application(s) → %d Secret(s) with preserved user_key."
                                    .formatted(sysName, appCount, apiKeySecrets.size()));
                } else if (productReady.applications() != null && !productReady.applications().isEmpty()) {
                    consolidationWarnings.add(
                            "Product '%s' (API Key): %d application(s) but no user_key — no consumer Secrets generated."
                                    .formatted(sysName, productReady.applications().size()));
                }
            } else {
                addOidcMigrationWarnings(sysName, productReady, consolidationWarnings);
            }

            if (ThreeScaleAuthMode.isTokenIntrospection(productReady.authentication())
                    && ThreeScaleAuthMode.resolveIntrospectionUrl(productReady.authentication()) == null) {
                consolidationWarnings.add(
                        "Product '%s' (OIDC introspection): token introspection endpoint not found — AuthPolicy uses placeholder introspection URL."
                                .formatted(sysName));
            }

            resources.add(authPolicyResourceGenerator.build(sysName + "-auth", ns, routeName, productReady, authMode));
            resources.add(rateLimitResourceGenerator.build(sysName + "-ratelimit", ns, routeName, productReady, derivedRateLimit));
            addSuggestedPolicyWarnings(sysName, productReady, gatewayStrategy, consolidationWarnings);

            if (observabilityEnabled) {
                resources.add(buildTelemetryPolicy(sysName + "-telemetry", ns, routeName, product));
            }

            resources.add(buildOpenShiftRoute(sysName, gatewayName));
        }

        String catalogInfo = developerHubEnabled ? buildCatalogInfo(products, gatewayStrategy, oasCache, resources) : null;

        String planId = UUID.randomUUID().toString().substring(0, 8);
        String effectiveClusterId = targetClusterId != null ? targetClusterId : "local";
        io.apishift.model.TargetCluster cluster = clusterRegistry.getCluster(effectiveClusterId);
        String clusterLabel = cluster != null ? cluster.label() : "Local (in-cluster)";

        sortResourcesForApply(resources);

        String aiAnalysis = runAiVerification(products, resources);

        List<MigrationPrerequisite> prerequisites = buildPrerequisites(resources, effectiveClusterId);

        MigrationPlan plan = new MigrationPlan(
                planId, gatewayStrategy,
                products.stream().map(ThreeScaleProduct::systemName).toList(),
                resources, aiAnalysis, Instant.now(),
                catalogInfo, "ACTIVE", effectiveClusterId, clusterLabel,
                consolidationWarnings, prerequisites
        );

        persistPlan(plan);
        metrics.stopMigrationTimer(timerSample);
        for (ThreeScaleProduct p : products) {
            metrics.recordMigration(p.systemName(), "analyzed");
        }
        return plan;
    }

    private ProductContext resolveProductContext(ThreeScaleProduct product, BackendEndpointResolver.BackendIndex backends) {
        List<ResolvedBackend> resolvedBackends = new ArrayList<>();
        String primarySvcName = null;
        String primarySvcNs = null;
        String primaryOas = null;
        List<ThreeScaleProduct.MappingRule> primaryRules = null;
        boolean hasRealOas = false;

        for (ThreeScaleProduct.BackendUsage usage : product.backendUsages()) {
            String endpoint = backends.lookup(usage.backendName());
            if (endpoint == null || endpoint.isBlank()) continue;

            String svcName = BackendEndpointResolver.parseServiceName(endpoint);
            String svcNs = BackendEndpointResolver.parseServiceNamespace(endpoint);
            String path = usage.path() != null && !usage.path().isBlank() ? usage.path() : "/";
            resolvedBackends.add(new ResolvedBackend(svcName, svcNs, path));

            if (primarySvcName == null) {
                primarySvcName = svcName;
                primarySvcNs = svcNs;

                String fullOas = openApiSynthesisService.fetchFullSpec(endpoint);
                if (fullOas != null) {
                    LOG.infof("Fetched real OpenAPI spec from %s for product %s", endpoint, product.systemName());
                    primaryOas = fullOas;
                    hasRealOas = true;
                } else {
                    List<ThreeScaleProduct.MappingRule> rules = openApiSynthesisService.fetchPaths(endpoint);
                    if (!rules.isEmpty()) {
                        primaryRules = rules;
                    }
                }
            }
        }

        if (primarySvcName == null) {
            return new ProductContext(null, product.systemName(), null, product.mappingRules(), false, List.of());
        }

        List<ThreeScaleProduct.MappingRule> effectiveRules = primaryRules != null ? primaryRules : product.mappingRules();
        return new ProductContext(primaryOas, primarySvcName, primarySvcNs, effectiveRules, hasRealOas, resolvedBackends);
    }

    private String patchKuadrantctlOutput(String yaml, String kind, String name,
            String namespace, String gatewayName, String gwNamespace,
            String productSysName, String hostname) {
        yaml = yaml.replace("\r\n", "\n").replace("\r", "\n");
        yaml = yaml.replaceAll("(?m)^\\s*creationTimestamp:\\s*null\\s*\\n", "");
        yaml = yaml.replaceAll("(?m)^\\s*status:\\s*\\n(\\s+\\S+.*\\n)*", "");

        String metadataBlock = "metadata:\n"
                + "  name: " + name + "\n"
                + "  namespace: " + namespace + "\n"
                + "  labels:\n"
                + "    app.kubernetes.io/managed-by: apishift\n"
                + "    \"apishift.io/product\": \"" + productSysName + "\"";
        yaml = yaml.replaceFirst("(?m)^metadata:(\\s*\\n(\\s+.*\\n)*?)(?=^\\S)", metadataBlock + "\n");
        if (!yaml.contains("  name: " + name)) {
            yaml = yaml.replaceFirst("(?m)^metadata:", metadataBlock);
        }

        if ("HTTPRoute".equals(kind) && gatewayName != null && !yaml.contains("parentRefs")) {
            String specBlock = "spec:\n"
                    + "  hostnames:\n"
                    + "    - " + hostname + "\n"
                    + "  parentRefs:\n"
                    + "    - name: " + gatewayName + "\n"
                    + "      namespace: " + gwNamespace;
            yaml = yaml.replaceFirst("(?m)^spec:", specBlock);
        }

        return yaml;
    }

    private String consolidateHttpRouteRules(String yaml, String backendSvcName, List<String> warnings) {
        long ruleCount = yaml.lines()
                .filter(line -> line.trim().startsWith("- matches:"))
                .count();

        if (ruleCount <= 16) return yaml;

        LOG.infof("HTTPRoute has %d rules (max 16), consolidating to prefix-based rules", ruleCount);

        int rulesStart = yaml.indexOf("\n  rules:\n");
        if (rulesStart < 0) return yaml;

        String rulesSection = yaml.substring(rulesStart);
        Set<String> prefixes = new LinkedHashSet<>();

        for (String line : rulesSection.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("value:")) {
                String val = trimmed.substring(6).trim().replaceAll("^[\"']|[\"']$", "");
                if (val.startsWith("/")) {
                    String[] segments = val.split("/", 3);
                    if (segments.length > 1 && !segments[1].isEmpty()) {
                        prefixes.add("/" + segments[1]);
                    } else {
                        prefixes.add("/");
                    }
                }
            }
        }

        if (prefixes.isEmpty()) prefixes.add("/");
        if (prefixes.size() > 16) {
            prefixes = new LinkedHashSet<>();
            prefixes.add("/");
        }

        warnings.add("HTTPRoute has " + ruleCount + " rules (max 16), consolidated to " + prefixes.size()
                + " prefix-based rules: " + prefixes);

        LOG.infof("Consolidated %d rules into %d prefix-based rules: %s", ruleCount, prefixes.size(), prefixes);

        StringBuilder rules = new StringBuilder();
        for (String prefix : prefixes) {
            rules.append("    - matches:\n");
            rules.append("        - path:\n");
            rules.append("            type: PathPrefix\n");
            rules.append("            value: ").append(prefix).append("\n");
            rules.append("      backendRefs:\n");
            rules.append("        - name: ").append(backendSvcName).append("\n");
            rules.append("          port: 8080\n");
        }

        return yaml.substring(0, rulesStart) + "\n  rules:\n" + rules;
    }

    private String runAiVerification(List<ThreeScaleProduct> products, List<MigrationPlan.GeneratedResource> resources) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Review this ApiShift migration plan. Verify correctness and provide a brief analysis.\n\n");
            prompt.append("Products being migrated: ");
            prompt.append(products.stream().map(ThreeScaleProduct::name).collect(Collectors.joining(", ")));
            prompt.append("\n\nGenerated resources summary:\n");

            for (MigrationPlan.GeneratedResource r : resources) {
                prompt.append("- ").append(r.kind()).append(": ").append(r.name())
                        .append(" (ns: ").append(r.namespace()).append(")\n");
            }

            long httpRouteCount = resources.stream().filter(r -> "HTTPRoute".equals(r.kind())).count();
            long authCount = resources.stream().filter(r -> "AuthPolicy".equals(r.kind())).count();
            long rlCount = resources.stream().filter(r -> "RateLimitPolicy".equals(r.kind())).count();

            prompt.append("\nSample HTTPRoute YAML:\n```yaml\n");
            resources.stream().filter(r -> "HTTPRoute".equals(r.kind())).findFirst()
                    .ifPresent(r -> prompt.append(r.yaml()));
            prompt.append("\n```\n");

            long secretCount = resources.stream().filter(r -> "Secret".equals(r.kind())).count();
            if (secretCount > 0) {
                prompt.append("\nConsumer API keys are preserved from 3scale user_key values in Kubernetes Secrets (not new APIKey CRDs).\n");
            }

            prompt.append("\nProvide: 1) Correctness assessment, 2) Potential issues, 3) Recommendations. Keep it concise (max 200 words).");

            String analysis = migrationAgent.chat(prompt.toString());
            if (analysis != null) {
                analysis = analysis.replaceAll("(?s)<think>.*?</think>\\s*", "").trim();
                int closeIdx = analysis.indexOf("</think>");
                if (closeIdx >= 0) analysis = analysis.substring(closeIdx + "</think>".length()).trim();
            }
            return analysis != null ? analysis : "AI verification unavailable";
        } catch (Exception e) {
            LOG.warnf("AI verification failed: %s", e.getMessage());
            return "AI verification skipped: " + e.getMessage();
        }
    }

    public List<AuditEntry> getAuditLog() {
        return auditRepository.findAllDescending();
    }

    public MigrationPlan getPlan(String planId) {
        return migrationPlanRepository.findPlanById(planId);
    }

    public List<MigrationPlan> listPlans() {
        return migrationPlanRepository.listPlanSummaries();
    }

    @Transactional
    public void addAuditEntry(AuditEntry entry) {
        auditRepository.save(entry);
    }

    @Transactional
    public void updatePlanStatus(String planId, String status) {
        migrationPlanRepository.updateStatus(planId, status);
    }

    @Transactional
    void persistPlan(MigrationPlan plan) {
        migrationPlanRepository.save(plan);
    }

    private List<MigrationPrerequisite> buildPrerequisites(
            List<MigrationPlan.GeneratedResource> resources, String targetClusterId) {
        List<MigrationPrerequisite> merged = new ArrayList<>();
        merged.addAll(prerequisiteCatalogService.fromPlan(resources, gatewayClassName, gatewayNamespace));
        merged.addAll(toolConfigPrerequisiteChecker.fromConfig(targetClusterId));
        merged = mergePrerequisitesById(merged);
        try {
            return clusterReadinessService.enrich(merged, targetClusterId);
        } catch (Exception e) {
            LOG.warn("Prerequisite enrichment failed; returning unknown statuses", e);
            return merged.stream().map(p -> p.withStatus("unknown")).toList();
        }
    }

    private List<MigrationPrerequisite> mergePrerequisitesById(List<MigrationPrerequisite> items) {
        Map<String, MigrationPrerequisite> byId = new LinkedHashMap<>();
        for (MigrationPrerequisite p : items) {
            byId.merge(p.id(), p, (a, b) -> new MigrationPrerequisite(
                    a.id(),
                    a.category(),
                    a.title(),
                    a.description(),
                    a.requiredByPlan() || b.requiredByPlan(),
                    a.optionalTier() || b.optionalTier(),
                    a.docUrl() != null ? a.docUrl() : b.docUrl(),
                    a.status(),
                    a.triggeredByCount() + b.triggeredByCount()
            ));
        }
        return new ArrayList<>(byId.values());
    }

    public String getCatalogInfoForProduct(String planId, String productName) {
        MigrationPlan plan = getPlan(planId);
        if (plan == null) return null;

        String sysName = sanitizeName(productName);
        String componentName = sysName + componentSuffix;
        String catalogYaml = plan.catalogInfoYaml();

        if (catalogYaml != null && !catalogYaml.isBlank()) {
            StringBuilder result = new StringBuilder();
            String[] docs = catalogYaml.split("(?m)^---\\s*$");
            for (String doc : docs) {
                String trimmed = doc.trim();
                if (trimmed.isEmpty()) continue;
                boolean isComponent = trimmed.contains("name: " + componentName);
                boolean isApiForProduct = trimmed.contains("kuadrant.io/apiproduct: " + sysName)
                        && trimmed.contains("kind: API");
                if (isComponent || isApiForProduct) {
                    if (result.length() > 0) result.append("\n---\n");
                    result.append(trimmed);
                }
            }
            if (result.length() > 0) return result.toString();
        }

        String routeName = sysName + "-route";
        String namespace = plan.resources().stream()
                .filter(r -> "HTTPRoute".equals(r.kind()) && r.name().equals(routeName))
                .findFirst().map(MigrationPlan.GeneratedResource::namespace).orElse(gatewayNamespace);
        String hostname = sysName + "." + clusterDomain;

        StringBuilder fallback = new StringBuilder();
        fallback.append("apiVersion: backstage.io/v1alpha1\n")
                .append("kind: Component\n")
                .append("metadata:\n")
                .append("  name: ").append(componentName).append("\n")
                .append("  namespace: default\n")
                .append("  description: \"").append(sysName).append(" — migrated from 3scale to Connectivity Link by ApiShift\"\n")
                .append("  annotations:\n")
                .append("    kuadrant.io/namespace: ").append(namespace).append("\n")
                .append("    kuadrant.io/httproute: ").append(routeName).append("\n")
                .append("    kuadrant.io/apiproduct: ").append(sysName).append("\n")
                .append("    apishift.io/managed-by: apishift\n")
                .append("    apishift.io/migration-plan-id: ").append(planId).append("\n")
                .append("    backstage.io/kubernetes-namespace: ").append(namespace).append("\n")
                .append("    backstage.io/kubernetes-id: ").append(sysName).append("\n")
                .append("    backstage.io/kubernetes-label-selector: \"app.kubernetes.io/managed-by=apishift,apishift.io/product=").append(sysName).append("\"\n")
                .append("    backstage.io/managed-by-origin-location: \"ApiShift:").append(sysName).append("\"\n")
                .append("  tags:\n")
                .append("    - connectivity-link\n")
                .append("    - kuadrant\n")
                .append("    - apishift-migrated\n")
                .append("spec:\n")
                .append("  type: service\n")
                .append("  lifecycle: production\n")
                .append("  owner: group:default/3scale\n")
                .append("  system: apishift-migrated-apis\n")
                .append("  providesApis:\n")
                .append("    - ").append(sysName).append("\n");
        return fallback.toString();
    }

    public List<TestCommand> generateTestCommands(String planId) {
        MigrationPlan plan = getPlan(planId);
        if (plan == null) return List.of();

        List<TestCommand> commands = new ArrayList<>();
        Set<String> httpRoutePaths = new LinkedHashSet<>();
        String authType = "api-key";

        for (MigrationPlan.GeneratedResource res : plan.resources()) {
            if ("HTTPRoute".equals(res.kind())) {
                extractPathsFromYaml(res.yaml(), httpRoutePaths);
            }
            if ("AuthPolicy".equals(res.kind())) {
                if (res.yaml().contains("jwt:") || res.yaml().contains("oidc")) {
                    authType = "oidc";
                }
            }
        }

        for (MigrationPlan.GeneratedResource res : plan.resources()) {
            if (!"Route".equals(res.kind()) || !res.yaml().contains("host:")) continue;

            String yaml = res.yaml();
            int idx = yaml.indexOf("host:");
            if (idx < 0) continue;
            String rest = yaml.substring(idx + 5).trim();
            String host = rest.split("\\s")[0].trim();

            commands.add(new TestCommand(
                    "Test " + res.name() + " (no auth — expect 401/403)",
                    "curl -sk https://" + host + "/",
                    "no-auth"));

            if ("oidc".equals(authType)) {
                commands.add(new TestCommand(
                        "Test " + res.name() + " with Bearer Token",
                        "curl -sk -H \"Authorization: Bearer $TOKEN\" https://" + host + "/",
                        "bearer"));
            } else {
                commands.add(new TestCommand(
                        "Test " + res.name() + " with API Key (header)",
                        "curl -sk -H \"api_key: YOUR_API_KEY\" https://" + host + "/",
                        "api-key"));
            }

            for (String path : httpRoutePaths) {
                if ("/".equals(path)) continue;
                String curlAuth = "oidc".equals(authType)
                        ? "-H \"Authorization: Bearer $TOKEN\""
                        : "-H \"api_key: YOUR_API_KEY\"";
                commands.add(new TestCommand(
                        "Test path " + path,
                        "curl -sk " + curlAuth + " https://" + host + path,
                        "path-test"));
            }
        }
        return commands;
    }

    private void extractPathsFromYaml(String yaml, Set<String> paths) {
        for (String line : yaml.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("value:")) {
                String val = trimmed.substring(6).trim().replaceAll("^[\"']|[\"']$", "");
                if (val.startsWith("/")) paths.add(val);
            }
        }
    }

    private String buildCatalogInfo(List<ThreeScaleProduct> products, String strategy,
            Map<String, String> oasCache, List<MigrationPlan.GeneratedResource> resources) {
        StringBuilder sb = new StringBuilder();
        String gfUrl = developerHubUrl.equals("none") ? "https://apishift." + clusterDomain : developerHubUrl;

        for (int i = 0; i < products.size(); i++) {
            ThreeScaleProduct p = products.get(i);
            String sysName = p.systemName();
            String routeName = sysName + "-route";
            String ns = resolveHttpRouteNamespace(sysName, resources);

            if (i > 0) sb.append("\n---\n");

            String desc = p.description() != null ? p.description().replace("\"", "'") : sysName;
            String hostname = sysName + "." + clusterDomain;

            sb.append("apiVersion: backstage.io/v1alpha1\n")
              .append("kind: Component\n")
              .append("metadata:\n")
              .append("  name: ").append(sysName).append("-product\n")
              .append("  namespace: default\n")
              .append("  description: \"").append(desc).append(" — migrated from 3scale to Connectivity Link by ApiShift\"\n")
              .append("  annotations:\n")
              .append("    kuadrant.io/namespace: ").append(ns).append("\n")
              .append("    kuadrant.io/httproute: ").append(routeName).append("\n")
              .append("    kuadrant.io/apiproduct: ").append(sysName).append("\n")
              .append("    apishift.io/managed-by: apishift\n")
              .append("    backstage.io/kubernetes-namespace: ").append(ns).append("\n")
              .append("    backstage.io/kubernetes-id: ").append(sysName).append("\n")
              .append("    backstage.io/kubernetes-label-selector: \"app.kubernetes.io/managed-by=apishift,apishift.io/product=").append(sysName).append("\"\n")
              .append("    backstage.io/managed-by-origin-location: \"ApiShift:").append(sysName).append("\"\n")
              .append("  tags:\n")
              .append("    - connectivity-link\n")
              .append("    - kuadrant\n")
              .append("    - apishift-migrated\n")
              .append("  links:\n")
              .append("    - title: API Gateway Endpoint\n")
              .append("      url: https://").append(hostname).append("\n")
              .append("    - title: ApiShift Dashboard\n")
              .append("      url: ").append(gfUrl).append("\n")
              .append("spec:\n")
              .append("  type: service\n")
              .append("  lifecycle: production\n")
              .append("  owner: group:default/3scale\n")
              .append("  system: apishift-migrated-apis\n")
              .append("  providesApis:\n")
              .append("    - ").append(sysName).append("\n");
        }
        return sb.toString();
    }

    private String resolveHttpRouteNamespace(String productSysName,
            List<MigrationPlan.GeneratedResource> resources) {
        String routeName = productSysName + "-route";
        if (resources != null) {
            for (var r : resources) {
                if ("HTTPRoute".equals(r.kind()) && routeName.equals(r.name())) {
                    return r.namespace() != null ? r.namespace() : gatewayNamespace;
                }
            }
        }
        return gatewayNamespace;
    }

    private String sanitizeName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }

    private void addOidcMigrationWarnings(
            String sysName, ThreeScaleProduct product, List<String> warnings) {

        List<ThreeScaleProduct.Application> apps = product.applications() != null
                ? product.applications() : List.of();
        int appCount = apps.size();
        long withClientId = apps.stream()
                .filter(a -> a.applicationId() != null && !a.applicationId().isBlank())
                .count();

        String issuer = ThreeScaleAuthMode.resolveJwtIssuerUrl(product.authentication());
        if (issuer == null || issuer.isBlank()) {
            warnings.add(
                    "Product '%s' (OIDC): oidc_issuer_endpoint not found in 3scale — AuthPolicy JWT issuer is a placeholder; set the real issuer before apply."
                            .formatted(sysName));
        } else {
            warnings.add(
                    ("Product '%s' (OIDC): %d application(s), %d with application_id (OAuth client_id). " +
                            "JWT issuer %s. Clients keep the same IdP token endpoint and Authorization: Bearer; no API key Secrets.")
                            .formatted(sysName, appCount, withClientId, issuer));
        }

        for (ThreeScaleProduct.Application app : apps) {
            if (app.applicationId() == null || app.applicationId().isBlank()) {
                warnings.add(
                        "Application '%s' (id %d, product %s) has no application_id — excluded from OIDC PlanPolicy tier matching."
                                .formatted(app.name(), app.id(), sysName));
            }
        }
    }

    /**
     * Preserves 3scale application user_key values as Authorino-compatible Secrets
     * (AuthPolicy selects secrets labeled {@code app: <systemName>}).
     */
    private List<MigrationPlan.GeneratedResource> buildConsumerApiKeySecrets(
            String sysName, String namespace, ThreeScaleProduct product, List<String> warnings) {

        List<MigrationPlan.GeneratedResource> secrets = new ArrayList<>();
        if (product.applications() == null || product.applications().isEmpty()) {
            return secrets;
        }

        Set<String> usedSecretNames = new HashSet<>();
        for (ThreeScaleProduct.Application app : product.applications()) {
            String userKey = app.userKey();
            if (userKey == null || userKey.isBlank()) {
                warnings.add("Application '%s' (id %d, product %s) has no 3scale user_key — skipped."
                        .formatted(app.name(), app.id(), sysName));
                continue;
            }

            String safeName = app.name().toLowerCase().replaceAll("[^a-z0-9-]", "-");
            if (safeName.isBlank()) safeName = "app";
            if (safeName.length() > 30) safeName = safeName.substring(0, 30);
            // Unique per 3scale application id (multiple apps per API / product)
            String secretName = truncateDnsLabel(sysName + "-apikey-" + app.id() + "-" + safeName);
            int suffix = 1;
            while (!usedSecretNames.add(secretName)) {
                secretName = truncateDnsLabel(sysName + "-apikey-" + app.id() + "-" + safeName + "-" + suffix++);
            }

            String planTier = app.planSystemName().isBlank() ? "default" : app.planSystemName();
            String escapedKey = escapeYamlDoubleQuoted(userKey);

            String yaml = """
                    apiVersion: v1
                    kind: Secret
                    metadata:
                      name: %s
                      namespace: %s
                      labels:
                        authorino.kuadrant.io/managed-by: authorino
                        app: %s
                        app.kubernetes.io/managed-by: apishift
                        apishift.io/product: "%s"
                      annotations:
                        secret.kuadrant.io/plan-id: "%s"
                        apishift.io/3scale-application-id: "%d"
                        apishift.io/3scale-application-name: "%s"
                        apishift.io/migrated-from: "3scale-user-key"
                    stringData:
                      api_key: "%s"
                    type: Opaque
                    """.formatted(secretName, namespace, sysName, sysName, planTier,
                    app.id(), escapeYamlDoubleQuoted(app.name()), escapedKey);

            secrets.add(new MigrationPlan.GeneratedResource("Secret", secretName, namespace, yaml));
        }
        return secrets;
    }

    private static String escapeYamlDoubleQuoted(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Kubernetes metadata.name must fit DNS subdomain rules (max 63 chars). */
    private static String truncateDnsLabel(String name) {
        if (name == null || name.isBlank()) return "apishift-apikey";
        String normalized = name.toLowerCase().replaceAll("[^a-z0-9.-]", "-").replaceAll("-+", "-");
        normalized = normalized.replaceAll("^-+|-+$", "");
        if (normalized.length() <= 63) return normalized;
        return normalized.substring(0, 63).replaceAll("-+$", "");
    }

    private static final Map<String, Integer> RESOURCE_APPLY_ORDER = Map.ofEntries(
            Map.entry("Gateway", 0),
            Map.entry("HTTPRoute", 10),
            Map.entry("APIProduct", 20),
            Map.entry("PlanPolicy", 30),
            Map.entry("Secret", 40),
            Map.entry("AuthPolicy", 50),
            Map.entry("RateLimitPolicy", 60),
            Map.entry("TelemetryPolicy", 70),
            Map.entry("Route", 80),
            Map.entry("APIKey", 90)
    );

    private void sortResourcesForApply(List<MigrationPlan.GeneratedResource> resources) {
        resources.sort(Comparator
                .comparingInt((MigrationPlan.GeneratedResource r) ->
                        RESOURCE_APPLY_ORDER.getOrDefault(r.kind(), 999))
                .thenComparing(MigrationPlan.GeneratedResource::namespace, Comparator.nullsFirst(String::compareTo))
                .thenComparing(MigrationPlan.GeneratedResource::name));
    }

    private MigrationPlan.GeneratedResource buildApiProduct(
            String name, String namespace, String routeName, ThreeScaleProduct product) {

        String desc = product.description() != null && !product.description().isBlank()
                ? product.description().replace("\"", "'")
                : product.name();

        String authType = ThreeScaleAuthMode.fromProduct(product) == ThreeScaleAuthMode.OIDC ? "oidc" : "api-key";

        String hostname = product.systemName() + "." + clusterDomain;

        String yaml = """
                apiVersion: devportal.kuadrant.io/v1alpha1
                kind: APIProduct
                metadata:
                  name: %s
                  namespace: %s
                  annotations:
                    backstage.io/owner: "group:default/3scale"
                    backstage.io/kubernetes-namespace: %s
                    backstage.io/kubernetes-id: %s
                  labels:
                    app.kubernetes.io/managed-by: apishift
                    "apishift.io/product": "%s"
                    backstage.io/kubernetes-id: %s
                spec:
                  targetRef:
                    group: gateway.networking.k8s.io
                    kind: HTTPRoute
                    name: %s
                  displayName: "%s"
                  description: |
                    %s
                    Migrated from 3scale to Connectivity Link by ApiShift.
                  version: "v1"
                  publishStatus: Published
                  approvalMode: automatic
                  tags:
                    - %s
                    - kuadrant
                    - apishift-migrated
                  contact:
                    team: platform-engineering
                    email: "platform@%s"
                  documentation:
                    openAPISpecURL: "https://%s/q/openapi"
                    swaggerUI: "https://%s/q/swagger-ui"
                """.formatted(name, namespace, namespace, product.systemName(), product.systemName(), product.systemName(),
                routeName, product.name(), desc, authType, clusterDomain, hostname, hostname);

        return new MigrationPlan.GeneratedResource("APIProduct", name, namespace, yaml);
    }

    private MigrationPlan.GeneratedResource buildTelemetryPolicy(
            String name, String namespace, String routeName, ThreeScaleProduct product) {

        String authType = ThreeScaleAuthMode.fromProduct(product) == ThreeScaleAuthMode.OIDC ? "oidc" : "api-key";

        String yaml = """
                apiVersion: extensions.kuadrant.io/v1alpha1
                kind: TelemetryPolicy
                metadata:
                  name: %s
                  namespace: %s
                  labels:
                    app.kubernetes.io/managed-by: apishift
                    "apishift.io/product": "%s"
                spec:
                  targetRef:
                    group: gateway.networking.k8s.io
                    kind: HTTPRoute
                    name: %s
                  metrics:
                    default:
                      labels:
                        product: '"%s"'
                        auth_type: '"%s"'
                        plan: 'auth.identity.metadata.annotations["secret.kuadrant.io/plan-id"]'
                        user: 'auth.identity.userid'
                """.formatted(name, namespace, product.systemName(), routeName,
                product.systemName(), authType);

        return new MigrationPlan.GeneratedResource("TelemetryPolicy", name, namespace, yaml);
    }

    private MigrationPlan.GeneratedResource buildOpenShiftRoute(String sysName, String gatewayName) {
        String hostname = sysName + "." + clusterDomain;
        String svcName = gatewayName + "-istio";
        String yaml = """
                apiVersion: route.openshift.io/v1
                kind: Route
                metadata:
                  name: %s
                  namespace: %s
                  labels:
                    app.kubernetes.io/managed-by: apishift
                spec:
                  host: %s
                  to:
                    kind: Service
                    name: %s
                    weight: 100
                  port:
                    targetPort: http
                  tls:
                    termination: edge
                    insecureEdgeTerminationPolicy: Redirect
                """.formatted(sysName, gatewayNamespace, hostname, svcName);
        return new MigrationPlan.GeneratedResource("Route", sysName, gatewayNamespace, yaml);
    }

    private void addSuggestedPolicyWarnings(
            String sysName, ThreeScaleProduct product, String gatewayStrategy, List<String> warnings) {

        Map<String, Object> auth = product.authentication();
        if (ThreeScaleAuthMode.suggestsBrowserOAuthFlow(auth)) {
            warnings.add(
                    "Product '%s': consider OIDCPolicy (extensions.kuadrant.io) for OAuth Authorization Code browser flow."
                            .formatted(sysName));
        }
        if (ThreeScaleAuthMode.suggestsTlsTermination(product)) {
            warnings.add(
                    "Product '%s': consider TLSPolicy for TLS termination; AuthPolicy x509/mTLS deferred."
                            .formatted(sysName));
        }
        if (ThreeScaleAuthMode.suggestsDnsPolicy(gatewayStrategy, product)) {
            warnings.add(
                    "Product '%s': dual gateway strategy — consider DNSPolicy for multicluster DNS exposure."
                            .formatted(sysName));
        }
    }
}
