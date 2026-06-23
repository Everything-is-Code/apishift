package io.apishift.service.generator;

import io.apishift.model.ThreeScaleProduct;
import io.apishift.service.ThreeScaleAuthMode;
import io.apishift.service.support.MigrationFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanPolicyResourceGeneratorTest {

    private final PlanPolicyResourceGenerator generator = new PlanPolicyResourceGenerator();

    @Test
    void build_apiKeyMode_emitsPublishedPlanTiers() {
        var product = MigrationFixtures.apiKeyProductWithPlans();

        var resource = generator.build("plan-demo", "default", "demo-route", product, ThreeScaleAuthMode.API_KEY);

        assertEquals("PlanPolicy", resource.kind());
        assertTrue(resource.yaml().contains("basic"));
    }

    @Test
    void build_oidcMode_emitsClientPredicates() {
        var product = new ThreeScaleProduct(
                "demo-api",
                "default",
                "demo-api",
                1L,
                "Demo API OIDC apps",
                "hosted",
                List.of(),
                List.of(),
                Map.of("type", "oidc"),
                "default",
                "default",
                "api-backend",
                null,
                List.of(new ThreeScaleProduct.ApplicationPlan(
                        10L, "Basic", "basic", "published", List.of())),
                List.of(new ThreeScaleProduct.Application(
                        42L, "test-app", "key", "Basic", "basic",
                        "dev@example.com", "client-42", "", "")));

        var resource = generator.build("plan-oidc", "default", "demo-route", product, ThreeScaleAuthMode.OIDC);

        assertTrue(resource.yaml().contains("PlanPolicy"));
        assertTrue(resource.yaml().contains("clientID"));
    }

    @Test
    void build_skipsDraftPlans() {
        var product = new ThreeScaleProduct(
                "demo-api", "default", "demo-api", 1L, "Demo", "hosted",
                List.of(), List.of(), Map.of(), "default", "default", "api-backend", null,
                List.of(new ThreeScaleProduct.ApplicationPlan(
                        10L, "Draft", "draft", "draft", List.of())),
                List.of());

        var resource = generator.build("plan-demo", "default", "demo-route", product, ThreeScaleAuthMode.API_KEY);

        assertTrue(resource.yaml().contains("PlanPolicy"));
        assertFalse(resource.yaml().contains("name: draft"));
    }
}
