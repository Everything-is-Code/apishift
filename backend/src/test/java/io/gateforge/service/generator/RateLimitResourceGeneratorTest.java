package io.gateforge.service.generator;

import io.gateforge.service.support.MigrationFixtures;
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
    void build_emitsRateLimitPolicyYaml() {
        var product = MigrationFixtures.apiKeyProductWithPlans();
        var derived = RateLimitResourceGenerator.deriveGlobalRateLimit(product);

        var resource = generator.build("rl-demo", "default", "demo-route", product, derived);

        assertEquals("RateLimitPolicy", resource.kind());
        assertTrue(resource.yaml().contains("limits:"));
        assertTrue(resource.yaml().contains("demo-api"));
    }
}
