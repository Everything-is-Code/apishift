package io.gateforge.service.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.gateforge.model.ThreeScaleProduct;
import io.gateforge.service.ThreeScaleAuthMode;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@ApplicationScoped
public class ThreeScaleExportParser {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final YAMLMapper yamlMapper = new YAMLMapper();

    public ExportParseResult parse(Path exportRoot) {
        if (exportRoot == null || !Files.isDirectory(exportRoot)) {
            throw new ExportParseException("Export path is not a directory: " + exportRoot);
        }
        try {
            ExportManifest manifest = ExportManifestValidator.validateAndRead(exportRoot, jsonMapper);
            ExportBackendIndex backends = ExportBackendIndex.load(exportRoot.resolve("backends"), jsonMapper);
            Map<Long, List<Map<String, Object>>> applicationsByService =
                    loadApplicationsByService(exportRoot.resolve("applications"));

            List<ThreeScaleProduct> products = new ArrayList<>();
            Path productsDir = exportRoot.resolve("products");
            if (!Files.isDirectory(productsDir)) {
                throw new ExportParseException("products/ directory is missing in export");
            }

            try (Stream<Path> children = Files.list(productsDir)) {
                for (Path child : children.filter(Files::isDirectory).sorted().toList()) {
                    Path proxyFile = child.resolve("proxy.json");
                    if (!Files.isRegularFile(proxyFile)) {
                        continue;
                    }
                    String systemName = child.getFileName().toString();
                    products.add(parseProduct(
                            productsDir,
                            systemName,
                            proxyFile,
                            backends,
                            manifest));
                }
            }

            for (ThreeScaleProduct product : products) {
                List<Map<String, Object>> apps = applicationsByService.getOrDefault(product.serviceId(), List.of());
                if (!apps.isEmpty()) {
                    int idx = products.indexOf(product);
                    products.set(idx, withApplications(product, apps));
                }
            }

            if (manifest.productCount() > 0 && products.isEmpty()) {
                throw new ExportParseException("manifest product_count > 0 but no products were parsed");
            }

            return new ExportParseResult(List.copyOf(products), backends, manifest);
        } catch (ExportParseException e) {
            throw e;
        } catch (IOException e) {
            throw new ExportParseException("Failed to parse export at " + exportRoot, e);
        }
    }

    private ThreeScaleProduct parseProduct(
            Path productsDir,
            String systemName,
            Path proxyFile,
            ExportBackendIndex backends,
            ExportManifest manifest) throws IOException {

        Map<String, Object> proxyDoc = jsonMapper.readValue(
                Files.readString(proxyFile), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> proxy = unwrap(proxyDoc.get("proxy"));
        long serviceId = toLong(proxy.get("service_id"));

        ProductYamlMetadata yamlMeta = readYamlMetadata(productsDir.resolve(systemName + ".yaml"));

        String displayName = yamlMeta.name() != null && !yamlMeta.name().isBlank()
                ? yamlMeta.name()
                : systemName;
        String resolvedSystemName = yamlMeta.systemName() != null && !yamlMeta.systemName().isBlank()
                ? yamlMeta.systemName()
                : systemName;

        List<ThreeScaleProduct.MappingRule> mappingRules = parseMappingRules(proxy);
        List<ThreeScaleProduct.BackendUsage> backendUsages =
                parseBackendUsages(productsDir.resolve(systemName), backends);
        Map<String, Object> auth = parseAuthentication(
                productsDir.resolve(systemName), proxy);
        List<ThreeScaleProduct.ApplicationPlan> plans =
                parseApplicationPlans(productsDir.resolve(systemName));

        String source = "export-v1 (" + manifest.adminUrl() + ")";
        return new ThreeScaleProduct(
                displayName,
                "export",
                resolvedSystemName,
                serviceId,
                "",
                "",
                mappingRules,
                backendUsages,
                auth,
                source,
                null,
                null,
                "offline",
                plans,
                List.of());
    }

    private ThreeScaleProduct withApplications(
            ThreeScaleProduct product,
            List<Map<String, Object>> rawApplications) {
        List<ThreeScaleProduct.Application> apps = new ArrayList<>();
        for (Map<String, Object> raw : rawApplications) {
            Map<String, Object> app = unwrap(raw.get("application"));
            long planId = toLong(app.get("plan_id"));
            String planName = "";
            String planSystemName = "";
            for (ThreeScaleProduct.ApplicationPlan plan : product.applicationPlans()) {
                if (plan.id() == planId) {
                    planName = plan.name();
                    planSystemName = plan.systemName();
                    break;
                }
            }
            apps.add(new ThreeScaleProduct.Application(
                    toLong(app.get("id")),
                    stringValue(app.get("name")),
                    stringValue(app.get("user_key")),
                    planName,
                    planSystemName,
                    stringValue(app.get("account_email")),
                    stringValue(app.get("application_id")),
                    stringValue(app.get("application_key")),
                    stringValue(app.get("redirect_url"))));
        }
        return new ThreeScaleProduct(
                product.name(),
                product.namespace(),
                product.systemName(),
                product.serviceId(),
                product.description(),
                product.deploymentOption(),
                product.mappingRules(),
                product.backendUsages(),
                product.authentication(),
                product.source(),
                product.backendNamespace(),
                product.backendServiceName(),
                product.sourceCluster(),
                product.applicationPlans(),
                apps);
    }

    private Map<Long, List<Map<String, Object>>> loadApplicationsByService(Path applicationsDir)
            throws IOException {
        Map<Long, List<Map<String, Object>>> byService = new LinkedHashMap<>();
        if (!Files.isDirectory(applicationsDir)) {
            return byService;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(applicationsDir, "page-*.json")) {
            for (Path page : stream) {
                List<Map<String, Object>> pageApps = jsonMapper.readValue(
                        Files.readString(page), new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> entry : pageApps) {
                    Map<String, Object> app = unwrap(entry.get("application"));
                    long serviceId = toLong(app.get("service_id"));
                    byService.computeIfAbsent(serviceId, ignored -> new ArrayList<>()).add(entry);
                }
            }
        }
        return byService;
    }

    private ProductYamlMetadata readYamlMetadata(Path yamlPath) {
        if (!Files.isRegularFile(yamlPath)) {
            return new ProductYamlMetadata(null, null);
        }
        try {
            Map<String, Object> root = yamlMapper.readValue(
                    Files.readString(yamlPath), new TypeReference<Map<String, Object>>() {});
            Map<String, Object> spec = extractProductSpec(root);
            return new ProductYamlMetadata(
                    stringValue(spec.get("name")),
                    firstNonBlank(
                            stringValue(spec.get("systemName")),
                            stringValue(spec.get("system_name"))));
        } catch (IOException e) {
            return new ProductYamlMetadata(null, null);
        }
    }

    private Map<String, Object> extractProductSpec(Map<String, Object> root) {
        if (root.containsKey("spec")) {
            return unwrap(root.get("spec"));
        }
        if ("List".equals(stringValue(root.get("kind"))) && root.get("items") instanceof List<?> items) {
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> doc)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) doc;
                if ("Product".equals(stringValue(map.get("kind")))) {
                    return unwrap(map.get("spec"));
                }
            }
        }
        return Map.of();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private List<ThreeScaleProduct.MappingRule> parseMappingRules(Map<String, Object> proxy) {
        Object rulesObj = proxy.get("mapping_rules");
        if (!(rulesObj instanceof List<?> rules)) {
            return List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1));
        }
        List<ThreeScaleProduct.MappingRule> parsed = new ArrayList<>();
        for (Object item : rules) {
            if (!(item instanceof Map<?, ?> rule)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> rm = (Map<String, Object>) rule;
            parsed.add(new ThreeScaleProduct.MappingRule(
                    stringValue(rm.getOrDefault("http_method", "GET")),
                    stringValue(rm.getOrDefault("pattern", "/")),
                    stringValue(rm.getOrDefault("metric_id", "hits")),
                    toInt(rm.getOrDefault("delta", 1))));
        }
        return parsed.isEmpty()
                ? List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1))
                : parsed;
    }

    private List<ThreeScaleProduct.BackendUsage> parseBackendUsages(
            Path productDir,
            ExportBackendIndex backends) throws IOException {
        Path usagesFile = productDir.resolve("backend_usages.json");
        if (!Files.isRegularFile(usagesFile)) {
            return List.of();
        }
        List<?> usages = readBackendUsageItems(usagesFile);
        List<ThreeScaleProduct.BackendUsage> parsed = new ArrayList<>();
        for (Object item : usages) {
            if (!(item instanceof Map<?, ?> wrapper)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = unwrap(((Map<String, Object>) wrapper).get("backend_usage"));
            long backendId = toLong(usage.get("backend_id"));
            String path = stringValue(usage.getOrDefault("path", "/"));
            parsed.add(new ThreeScaleProduct.BackendUsage(backends.resolveBackendName(backendId), path));
        }
        return parsed;
    }

    private List<?> readBackendUsageItems(Path usagesFile) throws IOException {
        Object root = jsonMapper.readValue(
                Files.readString(usagesFile), new TypeReference<Object>() {});
        if (root instanceof List<?> list) {
            return list;
        }
        if (root instanceof Map<?, ?> doc) {
            @SuppressWarnings("unchecked")
            Object usagesObj = ((Map<String, Object>) doc).get("backend_usages");
            if (usagesObj instanceof List<?> list) {
                return list;
            }
        }
        return List.of();
    }

    private Map<String, Object> parseAuthentication(Path productDir, Map<String, Object> proxy)
            throws IOException {
        Map<String, Object> auth = new LinkedHashMap<>();
        String authType = stringValue(proxy.get("auth_type"));
        if (!authType.isBlank()) {
            auth.put("auth_type", authType);
            auth.put("type", authType);
        } else if ("true".equalsIgnoreCase(stringValue(proxy.get("auth_user_key")))) {
            auth.put("type", "api_key");
        } else if ("true".equalsIgnoreCase(stringValue(proxy.get("auth_app_id")))) {
            auth.put("type", "app_id");
        }
        ThreeScaleAuthMode.enrichAuthFromProxy(auth, proxy);

        Path oidcFile = productDir.resolve("oidc_configuration.json");
        if (Files.isRegularFile(oidcFile)) {
            Map<String, Object> oidcDoc = jsonMapper.readValue(
                    Files.readString(oidcFile), new TypeReference<Map<String, Object>>() {});
            Map<String, Object> oidc = unwrap(oidcDoc.get("oidc_configuration"));
            if (oidc == null) {
                oidc = oidcDoc;
            }
            for (Map.Entry<String, Object> entry : oidc.entrySet()) {
                auth.putIfAbsent(entry.getKey(), entry.getValue());
            }
            if (ThreeScaleAuthMode.fromAuthMap(auth) == ThreeScaleAuthMode.OIDC) {
                auth.put("type", "oidc");
            }
        }
        return auth;
    }

    private List<ThreeScaleProduct.ApplicationPlan> parseApplicationPlans(Path productDir)
            throws IOException {
        Path plansFile = productDir.resolve("application_plans.json");
        if (!Files.isRegularFile(plansFile)) {
            return List.of();
        }
        Map<String, Object> doc = jsonMapper.readValue(
                Files.readString(plansFile), new TypeReference<Map<String, Object>>() {});
        Object plansObj = doc.get("plans");
        if (!(plansObj instanceof List<?> plans)) {
            return List.of();
        }
        List<ThreeScaleProduct.ApplicationPlan> parsed = new ArrayList<>();
        for (Object item : plans) {
            if (!(item instanceof Map<?, ?> wrapper)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> plan = unwrap(((Map<String, Object>) wrapper).get("application_plan"));
            parsed.add(new ThreeScaleProduct.ApplicationPlan(
                    toLong(plan.get("id")),
                    stringValue(plan.get("name")),
                    stringValue(plan.getOrDefault("system_name", plan.get("name"))),
                    stringValue(plan.getOrDefault("state", "published")),
                    List.of()));
        }
        return parsed;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record ProductYamlMetadata(String name, String systemName) {}
}
