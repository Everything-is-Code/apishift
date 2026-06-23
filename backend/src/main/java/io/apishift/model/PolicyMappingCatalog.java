package io.apishift.model;

import java.util.List;

/**
 * Authoritative RHCL 1.3 mapping reference for documentation, AI prompts, and API consumers.
 * Derived from the consolidated 3scale → Connectivity Link taxonomy (PPT audit + ApiShift implementation).
 */
public final class PolicyMappingCatalog {

    private PolicyMappingCatalog() {}

    public enum Layer {
        CONNECTIVITY,
        CORE_POLICY,
        EXTENSION,
        PORTAL,
        PLATFORM,
        MANUAL
    }

    /** How ApiShift handles this mapping today in {@code MigrationService.analyze}. */
    public enum ImplementationStatus {
        /** YAML is generated automatically during analyze. */
        GENERATED,
        /** Partially generated (placeholder or generic defaults). */
        PARTIAL,
        /** Documented suggestion only; not emitted in plans yet. */
        SUGGESTED,
        /** No RHCL equivalent; manual redesign required. */
        MANUAL,
        /** 3scale-specific; no migration target. */
        NOT_APPLICABLE
    }

    public record ConsolidatedMapping(
            String threescaleConcept,
            String rhclTarget,
            String apiGroup,
            Layer layer,
            ImplementationStatus apishiftStatus,
            String notes
    ) {}

    public record RhclTaxonomyLayer(
            String layer,
            String apiGroup,
            List<String> resources
    ) {}

    public record ApicastPolicyMapping(
            String apicastPolicy,
            String rhclTarget,
            ImplementationStatus apishiftStatus,
            String notes
    ) {}

    public static List<RhclTaxonomyLayer> taxonomy() {
        return List.of(
                new RhclTaxonomyLayer("Gateway API", "gateway.networking.k8s.io/v1",
                        List.of("Gateway", "HTTPRoute")),
                new RhclTaxonomyLayer("Core RHCL policies", "kuadrant.io/v1",
                        List.of("AuthPolicy", "RateLimitPolicy", "TokenRateLimitPolicy", "DNSPolicy", "TLSPolicy")),
                new RhclTaxonomyLayer("RHCL extensions", "extensions.kuadrant.io/v1alpha1",
                        List.of("PlanPolicy", "OIDCPolicy", "TelemetryPolicy")),
                new RhclTaxonomyLayer("Developer Portal", "devportal.kuadrant.io/v1alpha1",
                        List.of("APIProduct", "APIKey")),
                new RhclTaxonomyLayer("Platform", "v1 / route.openshift.io/v1",
                        List.of("Secret", "Route", "Service", "ServiceEntry"))
        );
    }

    /**
     * Single consolidated mapping table (section 6 — documentation / code reference).
     */
    public static List<ConsolidatedMapping> consolidated() {
        return List.of(
                new ConsolidatedMapping(
                        "Product / exposure",
                        "Gateway + HTTPRoute + Route",
                        "gateway.networking.k8s.io/v1, route.openshift.io/v1",
                        Layer.CONNECTIVITY,
                        ImplementationStatus.GENERATED,
                        "Gateway strategies: shared, dual, dedicated"),
                new ConsolidatedMapping(
                        "Backend / mapping rules",
                        "HTTPRoute.rules + backendRefs",
                        "gateway.networking.k8s.io/v1",
                        Layer.CONNECTIVITY,
                        ImplementationStatus.GENERATED,
                        "PathPrefix matches; max 16 rules consolidated; multi-backend per usage path"),
                new ConsolidatedMapping(
                        "Application (API Key)",
                        "Secret + AuthPolicy.apiKey",
                        "v1, kuadrant.io/v1",
                        Layer.CORE_POLICY,
                        ImplementationStatus.GENERATED,
                        "user_key preserved; Authorino labels app=<systemName>"),
                new ConsolidatedMapping(
                        "Application (OIDC)",
                        "AuthPolicy.jwt (no Secret)",
                        "kuadrant.io/v1",
                        Layer.CORE_POLICY,
                        ImplementationStatus.GENERATED,
                        "issuerUrl from oidc_issuer_endpoint; Bearer flow unchanged"),
                new ConsolidatedMapping(
                        "Application Plan + limits",
                        "PlanPolicy",
                        "extensions.kuadrant.io/v1alpha1",
                        Layer.EXTENSION,
                        ImplementationStatus.GENERATED,
                        "Primary vehicle for plan tiers; API Key and OIDC predicates"),
                new ConsolidatedMapping(
                        "Global / edge limit",
                        "RateLimitPolicy",
                        "kuadrant.io/v1",
                        Layer.CORE_POLICY,
                        ImplementationStatus.PARTIAL,
                        "Derived from plan minute/hour limits when available; placeholder 100 req/60s when no plans"),
                new ConsolidatedMapping(
                        "Token-based limit (LLM)",
                        "TokenRateLimitPolicy",
                        "kuadrant.io/v1",
                        Layer.CORE_POLICY,
                        ImplementationStatus.SUGGESTED,
                        "Suggest when API is token-metered (LLM workloads)"),
                new ConsolidatedMapping(
                        "TLS termination",
                        "TLSPolicy",
                        "kuadrant.io/v1",
                        Layer.CORE_POLICY,
                        ImplementationStatus.SUGGESTED,
                        "Recommend when APICast/custom TLS config detected; cert-manager/ACME"),
                new ConsolidatedMapping(
                        "DNS / multicluster",
                        "DNSPolicy",
                        "kuadrant.io/v1",
                        Layer.CORE_POLICY,
                        ImplementationStatus.SUGGESTED,
                        "Recommend for multicluster or custom DNS exposure"),
                new ConsolidatedMapping(
                        "Custom metrics labels",
                        "TelemetryPolicy",
                        "extensions.kuadrant.io/v1alpha1",
                        Layer.EXTENSION,
                        ImplementationStatus.GENERATED,
                        "When ApiShift.observability.enabled=true"),
                new ConsolidatedMapping(
                        "OAuth browser flow",
                        "OIDCPolicy",
                        "extensions.kuadrant.io/v1alpha1",
                        Layer.EXTENSION,
                        ImplementationStatus.SUGGESTED,
                        "Orchestrates OAuth Authorization Code flow; not a substitute for AuthPolicy JWT"),
                new ConsolidatedMapping(
                        "API catalog / discovery",
                        "APIProduct",
                        "devportal.kuadrant.io/v1alpha1",
                        Layer.PORTAL,
                        ImplementationStatus.PARTIAL,
                        "Generated only when ApiShift.developer-hub.enabled=true"),
                new ConsolidatedMapping(
                        "Custom Lua policies",
                        "EnvoyFilter / WASM Extension SDK",
                        "istio.io / manual",
                        Layer.MANUAL,
                        ImplementationStatus.MANUAL,
                        "No automatic migration; evaluate custom WASM or EnvoyFilter"),
                new ConsolidatedMapping(
                        "Header / URL rewrite",
                        "HTTPRoute filters",
                        "gateway.networking.k8s.io/v1",
                        Layer.CONNECTIVITY,
                        ImplementationStatus.SUGGESTED,
                        "Gateway API filters replace APICast URL/header rewrite policies")
        );
    }

    /**
     * 3scale APIcast built-in policies → RHCL 1.3 (slides 21–25 reference).
     */
    public static List<ApicastPolicyMapping> apicastPolicies() {
        return List.of(
                new ApicastPolicyMapping("API Key", "AuthPolicy apiKey + Secret", ImplementationStatus.GENERATED,
                        "Per-application user_key migration"),
                new ApicastPolicyMapping("JWT Claim Check", "AuthPolicy authorization (patternMatching / OPA)",
                        ImplementationStatus.SUGGESTED, "Post-auth authorization on JWT claims"),
                new ApicastPolicyMapping("OAuth 2.0 Token Introspection", "AuthPolicy oauth2Introspection",
                        ImplementationStatus.PARTIAL, "AuthPolicy oauth2Introspection when proxy uses introspection"),
                new ApicastPolicyMapping("RH-SSO / Keycloak Role Check", "AuthPolicy authorization on realm_access.roles",
                        ImplementationStatus.SUGGESTED, "See Kuadrant OIDC+RBAC guide"),
                new ApicastPolicyMapping("OIDC (Bearer JWT)", "AuthPolicy jwt.issuerUrl",
                        ImplementationStatus.GENERATED, "Not OIDCPolicy — JWT validation only"),
                new ApicastPolicyMapping("OAuth Authorization Code (browser)", "OIDCPolicy extension",
                        ImplementationStatus.SUGGESTED, "Browser redirect flow orchestration"),
                new ApicastPolicyMapping("OAuth 2.0 Mutual TLS Client Auth", "TLSPolicy + AuthPolicy x509",
                        ImplementationStatus.SUGGESTED, "RFC8705 partial; cert attrs limited"),
                new ApicastPolicyMapping("TLS Client Certificate Validation", "TLSPolicy + AuthPolicy x509",
                        ImplementationStatus.SUGGESTED, "Defense-in-depth L4+L7"),
                new ApicastPolicyMapping("Edge Limiting", "RateLimitPolicy + PlanPolicy",
                        ImplementationStatus.PARTIAL, "PlanPolicy for tiers; RateLimitPolicy for global ceiling"),
                new ApicastPolicyMapping("Custom Metrics", "TelemetryPolicy + Envoy/Istio metrics",
                        ImplementationStatus.PARTIAL, "Not 1:1 replacement for 3scale analytics"),
                new ApicastPolicyMapping("Web Assembly Plug-In", "Envoy WASM / Extension SDK",
                        ImplementationStatus.MANUAL, "Custom Lua → WASM custom policy; no auto-migrator"),
                new ApicastPolicyMapping("3scale Auth Caching / Batcher / Referrer", "N/A",
                        ImplementationStatus.NOT_APPLICABLE, "3scale architecture-specific"),
                new ApicastPolicyMapping("Content Caching, CORS, Retry, SOAP", "HTTPRoute filters / Istio / WASM",
                        ImplementationStatus.MANUAL, "Design-time strategy, no 1:1 CRD")
        );
    }

    public record PolicyMappingReference(
            List<RhclTaxonomyLayer> taxonomy,
            List<ConsolidatedMapping> consolidated,
            List<ApicastPolicyMapping> apicastPolicies
    ) {}

    public static PolicyMappingReference reference() {
        return new PolicyMappingReference(taxonomy(), consolidated(), apicastPolicies());
    }
}
