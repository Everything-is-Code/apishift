package io.gateforge.service.support;

import io.gateforge.model.ThreeScaleProduct;

import java.util.List;
import java.util.Map;

public final class MigrationFixtures {

    private MigrationFixtures() {}

    public static ThreeScaleProduct apiKeyProduct() {
        return new ThreeScaleProduct(
                "demo-api",
                "default",
                "demo-api",
                1L,
                "Demo API",
                "hosted",
                List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1)),
                List.of(new ThreeScaleProduct.BackendUsage("api", "/")),
                Map.of(),
                "default",
                "default",
                "api-backend",
                null,
                List.of(),
                List.of());
    }

    public static ThreeScaleProduct otherApiKeyProduct() {
        return new ThreeScaleProduct(
                "other-api",
                "default",
                "other-api",
                2L,
                "Other API",
                "hosted",
                List.of(new ThreeScaleProduct.MappingRule("GET", "/other", "hits", 1)),
                List.of(new ThreeScaleProduct.BackendUsage("other-backend", "/other")),
                Map.of(),
                "default",
                "default",
                "other-backend",
                null,
                List.of(),
                List.of());
    }

    public static ThreeScaleProduct oidcWithoutIssuer() {
        return new ThreeScaleProduct(
                "demo-api",
                "default",
                "demo-api",
                1L,
                "Demo API OIDC",
                "hosted",
                List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1)),
                List.of(new ThreeScaleProduct.BackendUsage("api", "/")),
                Map.of("type", "oidc"),
                "default",
                "default",
                "api-backend",
                null,
                List.of(),
                List.of());
    }

    public static ThreeScaleProduct oidcWithIssuer() {
        return new ThreeScaleProduct(
                "demo-api",
                "default",
                "demo-api",
                1L,
                "Demo API OIDC issuer",
                "hosted",
                List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1)),
                List.of(new ThreeScaleProduct.BackendUsage("api", "/")),
                Map.of("type", "oidc", "issuerUrl", "https://issuer.example.com/realms/demo"),
                "default",
                "default",
                "api-backend",
                null,
                List.of(),
                List.of());
    }

    public static ThreeScaleProduct apiKeyWithApplications() {
        return new ThreeScaleProduct(
                "demo-api",
                "default",
                "demo-api",
                1L,
                "Demo API with apps",
                "hosted",
                List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1)),
                List.of(new ThreeScaleProduct.BackendUsage("api", "/")),
                Map.of(),
                "default",
                "default",
                "api-backend",
                null,
                List.of(),
                List.of(new ThreeScaleProduct.Application(
                        42L,
                        "test-app",
                        "userkey-secret-value",
                        "Basic",
                        "basic",
                        "dev@example.com",
                        "",
                        "",
                        "")));
    }

    public static ThreeScaleProduct apiKeyProductWithPlans() {
        return new ThreeScaleProduct(
                "demo-api",
                "default",
                "demo-api",
                1L,
                "Demo API with plans",
                "hosted",
                List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1)),
                List.of(new ThreeScaleProduct.BackendUsage("api", "/")),
                Map.of(),
                "default",
                "default",
                "api-backend",
                null,
                List.of(new ThreeScaleProduct.ApplicationPlan(
                        10L,
                        "Basic",
                        "basic",
                        "published",
                        List.of(new ThreeScaleProduct.PlanLimit("hits", "minute", 60)))),
                List.of());
    }

    public static ThreeScaleProduct oidcIntrospectionProduct() {
        return new ThreeScaleProduct(
                "demo-api",
                "default",
                "demo-api",
                1L,
                "Demo API introspection",
                "hosted",
                List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1)),
                List.of(new ThreeScaleProduct.BackendUsage("api", "/")),
                Map.of(
                        "type", "oidc",
                        "auth_type", "introspection",
                        "token_introspection_endpoint",
                        "https://sso.example.com/realms/api/protocol/openid-connect/token/introspect"),
                "default",
                "default",
                "api-backend",
                null,
                List.of(),
                List.of());
    }
}
