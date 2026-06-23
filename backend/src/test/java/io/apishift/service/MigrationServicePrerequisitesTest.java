package io.apishift.service;

import io.apishift.model.MigrationPlan;
import io.apishift.service.support.MigrationFixtures;
import io.apishift.service.support.MigrationServiceTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationServicePrerequisitesTest extends MigrationServiceTestSupport {

    private MigrationServiceForTest migrationService;

    @BeforeEach
    void setUp() {
        migrationService = MigrationServiceForTest.createWithProducts(
                List.of(MigrationFixtures.apiKeyProduct()));
    }

    @Test
    void analyze_disconnectedCluster_returnsPrerequisitesWithUnknownStatus() {
        MigrationPlan plan = migrationService.analyze("shared", List.of("demo-api"), "local");

        assertNotNull(plan);
        assertFalse(plan.resources().isEmpty());
        assertNotNull(plan.prerequisites());
        assertFalse(plan.prerequisites().isEmpty());

        assertTrue(plan.prerequisites().stream().anyMatch(p -> "gateway-api".equals(p.id())));
        assertTrue(plan.prerequisites().stream().anyMatch(p -> "rhcl-core".equals(p.id())));
        assertTrue(plan.prerequisites().stream().anyMatch(p -> "apishift-cluster-api".equals(p.id())));

        plan.prerequisites().forEach(p -> assertEquals("unknown", p.status(),
                "Expected unknown for " + p.id() + " but was " + p.status()));
    }
}
