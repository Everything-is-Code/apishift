package io.gateforge.service;

import io.gateforge.model.MigrationPlan;
import io.gateforge.service.support.MigrationFixtures;
import io.gateforge.service.support.MigrationServiceTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
