package io.gateforge.service.support;

import io.gateforge.model.ThreeScaleProduct;

import java.util.List;
import java.util.Map;

public final class MinimalThreeScaleFixtures {

    private MinimalThreeScaleFixtures() {}

    public static ThreeScaleProduct minimalProduct() {
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
}
