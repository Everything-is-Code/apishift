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
}
