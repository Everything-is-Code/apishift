package io.apishift.service.generator;

import io.apishift.service.support.MigrationFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitResourceGeneratorTest {

    private final RateLimitResourceGenerator generator = new RateLimitResourceGenerator();

    @Test
    void deriveGlobalRateLimit_prefersMinuteLimits() {
        var derived = RateLimitResourceGenerator.deriveGlobalRateLimit(MigrationFixtures.apiKeyProductWithPlans());

        assertEquals(60L, derived.limit());
        assertEquals("1m", derived.window());
        assertEquals(false, derived.placeholder());
    }

    @Test
    void deriveGlobalRateLimit_withoutLimits_usesPlaceholder() {
        var derived = RateLimitResourceGenerator.deriveGlobalRateLimit(MigrationFixtures.apiKeyProduct());

        assertEquals(100L, derived.limit());
        assertEquals("60s", derived.window());
        assertTrue(derived.placeholder());
    }

    @Test
    void deriveGlobalRateLimit_prefersHourLimitsWhenNoMinute() {
        var product = new io.apishift.model.ThreeScaleProduct(
                "demo-api", "default", "demo-api", 1L, "Demo", "hosted",
                java.util.List.of(), java.util.List.of(), java.util.Map.of(),
                "default", "default", "api-backend", null,
                java.util.List.of(new io.apishift.model.ThreeScaleProduct.ApplicationPlan(
                        10L, "Basic", "basic", "published",
                        java.util.List.of(new io.apishift.model.ThreeScaleProduct.PlanLimit("hits", "hour", 500)))),
                java.util.List.of());

        var derived = RateLimitResourceGenerator.deriveGlobalRateLimit(product);

        assertEquals(500L, derived.limit());
        assertEquals("1h", derived.window());
    }

    @Test
    void build_emitsRateLimitPolicyYaml() {
        var product = MigrationFixtures.apiKeyProductWithPlans();
        var derived = RateLimitResourceGenerator.deriveGlobalRateLimit(product);

        var resource = generator.build("rl-demo", "default", "demo-route", product, derived);

        assertEquals("RateLimitPolicy", resource.kind());
        assertTrue(resource.yaml().contains("limits:"));
        assertTrue(resource.yaml().contains("demo-api"));
    }
}
