package io.apishift.service;

import io.apishift.model.MigrationPlan;
import io.apishift.model.MigrationPrerequisite;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

@ApplicationScoped
public class PrerequisiteCatalogService {

    private record PrerequisiteDef(
            String id,
            String category,
            String title,
            String description,
            boolean optionalTier,
            String docUrl
    ) {}

    private static final Map<String, PrerequisiteDef> DEFINITIONS = Map.ofEntries(
            Map.entry("gateway-api", new PrerequisiteDef(
                    "gateway-api", "connectivity",
                    "Gateway API",
                    "Gateway API CRDs and a GatewayClass matching apishift.connectivity-link.gateway-class-name.",
                    false,
                    "https://gateway-api.sigs.k8s.io/")),
            Map.entry("rhcl-core", new PrerequisiteDef(
                    "rhcl-core", "core-policy",
                    "RHCL / Kuadrant core",
                    "Kuadrant core operators (Authorino, Limitador) and kuadrant.io policy CRDs.",
                    false,
                    "https://docs.kuadrant.io/")),
            Map.entry("kuadrant-extensions", new PrerequisiteDef(
                    "kuadrant-extensions", "extension",
                    "Kuadrant extensions",
                    "extensions.kuadrant.io CRDs for PlanPolicy, OIDCPolicy, and TelemetryPolicy.",
                    false,
                    "https://docs.kuadrant.io/")),
            Map.entry("developer-portal", new PrerequisiteDef(
                    "developer-portal", "portal",
                    "Developer Portal",
                    "Developer Portal Controller and devportal.kuadrant.io APIProduct CRD.",
                    true,
                    "https://docs.kuadrant.io/")),
            Map.entry("openshift-route", new PrerequisiteDef(
                    "openshift-route", "platform",
                    "OpenShift Routes",
                    "OpenShift Route API for external host exposure when Route resources are generated.",
                    false,
                    "https://docs.openshift.com/")),
            Map.entry("authorino-secrets", new PrerequisiteDef(
                    "authorino-secrets", "core-policy",
                    "Authorino API key secrets",
                    "Authorino secret reconciliation for migrated API key credentials.",
                    false,
                    "https://docs.kuadrant.io/"))
    );

    public List<MigrationPrerequisite> fromPlan(
            List<MigrationPlan.GeneratedResource> resources,
            String gatewayClassName,
            String targetNamespace) {

        Map<String, Integer> counts = new LinkedHashMap<>();

        for (MigrationPlan.GeneratedResource resource : resources) {
            String kind = resource.kind();
            String yaml = resource.yaml() != null ? resource.yaml() : "";
            String apiVersion = extractApiVersion(yaml);

            switch (kind) {
                case "Gateway", "HTTPRoute" -> bump(counts, "gateway-api");
                case "AuthPolicy", "RateLimitPolicy", "Kuadrant" -> bump(counts, "rhcl-core");
                case "PlanPolicy", "TelemetryPolicy", "OIDCPolicy" -> bump(counts, "kuadrant-extensions");
                case "APIProduct" -> bump(counts, "developer-portal");
                case "Route" -> bump(counts, "openshift-route");
                case "Secret" -> {
                    if (yaml.contains("api_key") || yaml.contains("authorino")) {
                        bump(counts, "authorino-secrets");
                    }
                }
                default -> { }
            }

            if (apiVersion != null && apiVersion.contains("extensions.kuadrant.io")) {
                bump(counts, "kuadrant-extensions");
            }
            if (apiVersion != null && apiVersion.contains("devportal.kuadrant.io")) {
                bump(counts, "developer-portal");
            }
            if (apiVersion != null && apiVersion.contains("kuadrant.io/v1")) {
                bump(counts, "rhcl-core");
            }
        }

        if (counts.containsKey("gateway-api") && gatewayClassName != null && !gatewayClassName.isBlank()) {
            // GatewayClass is part of gateway-api prerequisite context; no separate id.
        }

        List<MigrationPrerequisite> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            PrerequisiteDef def = DEFINITIONS.get(entry.getKey());
            if (def == null) continue;
            result.add(toPrerequisite(def, entry.getValue(), "unknown"));
        }
        return result;
    }

    public MigrationPrerequisite definitionForId(String id, int count, String status) {
        PrerequisiteDef def = DEFINITIONS.get(id);
        if (def == null) return null;
        return toPrerequisite(def, count, status);
    }

    public Set<String> probeableIds() {
        return Set.of("gateway-api", "rhcl-core", "kuadrant-extensions", "developer-portal", "openshift-route");
    }

    public Optional<String> crdNameForId(String id) {
        return switch (id) {
            case "gateway-api" -> Optional.of("gateways.gateway.networking.k8s.io");
            case "rhcl-core" -> Optional.of("authpolicies.kuadrant.io");
            case "kuadrant-extensions" -> Optional.of("planpolicies.extensions.kuadrant.io");
            case "developer-portal" -> Optional.of("apiproducts.devportal.kuadrant.io");
            case "openshift-route" -> Optional.of("routes.route.openshift.io");
            default -> Optional.empty();
        };
    }

    private static void bump(Map<String, Integer> counts, String id) {
        counts.merge(id, 1, Integer::sum);
    }

    private static MigrationPrerequisite toPrerequisite(PrerequisiteDef def, int count, String status) {
        return new MigrationPrerequisite(
                def.id(), def.category(), def.title(), def.description(),
                true, def.optionalTier(), def.docUrl(), status, count);
    }

    static String extractApiVersion(String yaml) {
        if (yaml == null || yaml.isBlank()) return null;
        for (String line : yaml.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("apiVersion:")) {
                return trimmed.substring("apiVersion:".length()).trim();
            }
        }
        return null;
    }
}
