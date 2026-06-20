package io.gateforge.service;

import io.gateforge.model.MigrationPlan;
import io.gateforge.model.ThreeScaleProduct;
import io.gateforge.service.support.MigrationFixtures;
import io.gateforge.service.support.MigrationServiceTestSupport;
import io.gateforge.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MigrationServiceTest extends MigrationServiceTestSupport {

    @Test
    void analyze_shared_generatesCoreResources() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProduct()));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        assertNotNull(plan);
        assertTrue(hasKind(plan, "Gateway"));
        assertTrue(hasKind(plan, "HTTPRoute"));
        assertTrue(hasKind(plan, "AuthPolicy"));
        assertTrue(hasKind(plan, "RateLimitPolicy"));
        assertTrue(hasKind(plan, "Route"));
        assertEquals("gateforge-shared", resourceNames(plan, "Gateway").get(0));
    }

    @Test
    void analyze_dual_generatesTwoGateways() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProduct()));

        MigrationPlan plan = service.analyze("dual", List.of("demo-api"), "local");

        assertEquals(2, countKind(plan, "Gateway"));
        List<String> gatewayNames = resourceNames(plan, "Gateway");
        assertTrue(gatewayNames.contains("gateforge-internal"));
        assertTrue(gatewayNames.contains("gateforge-external"));
    }

    @Test
    void analyze_dedicated_generatesPerProductGateway() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProduct()));

        MigrationPlan plan = service.analyze("dedicated", List.of("demo-api"), "local");

        assertTrue(resourceNames(plan, "Gateway").contains("demo-api-gw"));
    }

    @Test
    void analyze_oidcMissingIssuer_addsWarning() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.oidcWithoutIssuer()));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        assertTrue(warningsContain(plan, "oidc_issuer_endpoint"));
        assertTrue(hasKind(plan, "AuthPolicy"));
    }

    @Test
    void analyze_apiKeyWithApplications_generatesSecrets() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyWithApplications()));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        assertTrue(hasKind(plan, "Secret"));
        assertTrue(warningsContain(plan, "user_key"));
        assertEquals(1, countKind(plan, "Secret"));
    }

    @Test
    void analyze_derivesRateLimitFromPlanLimits() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProductWithPlans()));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        String rateLimitYaml = resourceYaml(plan, "RateLimitPolicy");
        assertTrue(rateLimitYaml.contains("limit: 60"));
        assertTrue(rateLimitYaml.contains("window: 1m"));
        assertFalse(rateLimitYaml.contains("limit: 100"));
    }

    @Test
    void analyze_placeholderRateLimit_whenNoPlans() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProduct()));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        String rateLimitYaml = resourceYaml(plan, "RateLimitPolicy");
        assertTrue(rateLimitYaml.contains("limit: 100"));
        assertTrue(rateLimitYaml.contains("window: 60s"));
        assertTrue(warningsContain(plan, "placeholder 100 req/60s"));
    }

    @Test
    void analyze_skipsApiProduct_whenHubDisabled() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProduct()), false);

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        assertFalse(hasKind(plan, "APIProduct"));
        assertTrue(warningsContain(plan, "APIProduct skipped"));
    }

    @Test
    void analyze_includesApiProduct_whenHubEnabled() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProduct()), true);

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        assertTrue(hasKind(plan, "APIProduct"));
    }

    @Test
    void analyze_introspectionAuth_usesOauth2Introspection() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.oidcIntrospectionProduct()));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        String authYaml = resourceYaml(plan, "AuthPolicy");
        assertTrue(authYaml.contains("oauth2Introspection"));
        assertTrue(authYaml.contains("token/introspect"));
    }

    @Test
    void analyze_suggestedWarnings_forDualStrategy() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProduct()));

        MigrationPlan plan = service.analyze("dual", List.of("demo-api"), "local");

        assertTrue(warningsContain(plan, "DNSPolicy"));
    }

    @Test
    void analyze_oidcWithIssuer_buildsJwtAuthPolicy() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.oidcWithIssuer()));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        String authYaml = resourceYaml(plan, "AuthPolicy");
        assertTrue(authYaml.contains("jwt:"));
        assertTrue(authYaml.contains("issuerUrl"));
    }

    @Test
    void analyze_tlsDeployment_addsSuggestedWarning() {
        ThreeScaleProduct tlsProduct = new ThreeScaleProduct(
                "demo-api", "default", "demo-api", 1L, "Demo", "custom-ssl",
                List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1)),
                List.of(new ThreeScaleProduct.BackendUsage("api", "/")),
                Map.of(), "default", "default", "api-backend", null, List.of(), List.of());
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(List.of(tlsProduct));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        assertTrue(warningsContain(plan, "TLS"));
    }

    @Test
    void analyze_multipleProducts_generatesPerProductRoutes() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProduct(), MigrationFixtures.otherApiKeyProduct()));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api", "other-api"), "local");

        assertEquals(2, countKind(plan, "HTTPRoute"));
        assertTrue(resourceNames(plan, "HTTPRoute").contains("demo-api-route"));
        assertTrue(resourceNames(plan, "HTTPRoute").contains("other-api-route"));
    }

    @Test
    void analyze_browserOAuthFlow_addsSuggestedWarning() {
        ThreeScaleProduct browserOidc = new ThreeScaleProduct(
                "demo-api", "default", "demo-api", 1L, "Demo", "hosted",
                List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1)),
                List.of(new ThreeScaleProduct.BackendUsage("api", "/")),
                Map.of("type", "oidc", "oidc_issuer_type", "authorization_code"),
                "default", "default", "api-backend", null, List.of(), List.of());
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(List.of(browserOidc));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        assertTrue(warningsContain(plan, "OIDCPolicy"));
    }

    @Test
    void analyze_observabilityEnabled_addsTelemetryPolicy() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProduct()));
        ReflectionTestSupport.inject(service, "observabilityEnabled", true);

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        assertTrue(hasKind(plan, "TelemetryPolicy"));
    }

    @Test
    void analyze_oidcIntrospectionWithoutEndpoint_addsWarning() {
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(
                List.of(new ThreeScaleProduct(
                        "demo-api", "default", "demo-api", 1L, "Demo", "hosted",
                        List.of(new ThreeScaleProduct.MappingRule("GET", "/", "hits", 1)),
                        List.of(new ThreeScaleProduct.BackendUsage("api", "/")),
                        Map.of("type", "oidc", "auth_type", "introspection"),
                        "default", "default", "api-backend", null, List.of(), List.of())));

        MigrationPlan plan = service.analyze("shared", List.of("demo-api"), "local");

        assertTrue(warningsContain(plan, "introspection endpoint not found"));
    }
}
