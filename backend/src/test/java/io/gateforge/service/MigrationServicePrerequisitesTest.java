package io.gateforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.model.MigrationPlan;
import io.gateforge.service.support.ClusterRegistryStub;
import io.gateforge.service.support.MinimalThreeScaleFixtures;
import io.gateforge.service.support.ReflectionTestSupport;
import io.gateforge.service.support.TestDoubles;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationServicePrerequisitesTest {

    private MigrationServiceForTest migrationService;

    @BeforeEach
    void setUp() {
        migrationService = new MigrationServiceForTest();

        PrerequisiteCatalogService catalogService = new PrerequisiteCatalogService();
        ToolConfigPrerequisiteChecker toolConfigChecker = new ToolConfigPrerequisiteChecker();
        ClusterReadinessService readinessService = new ClusterReadinessService();

        ReflectionTestSupport.inject(toolConfigChecker, "clusterRegistry", ClusterRegistryStub.disconnected());
        ReflectionTestSupport.inject(toolConfigChecker, "clusterDomain", "apps.cluster.example.com");
        ReflectionTestSupport.inject(readinessService, "clusterRegistry", ClusterRegistryStub.disconnected());
        ReflectionTestSupport.inject(readinessService, "catalogService", catalogService);

        ReflectionTestSupport.inject(migrationService, "threeScaleService",
                TestDoubles.threeScaleService(List.of(MinimalThreeScaleFixtures.minimalProduct())));
        ReflectionTestSupport.inject(migrationService, "sourceRegistry", TestDoubles.emptySourceRegistry());
        ReflectionTestSupport.inject(migrationService, "clusterRegistry", ClusterRegistryStub.disconnected());
        ReflectionTestSupport.inject(migrationService, "objectMapper", new ObjectMapper());
        ReflectionTestSupport.inject(migrationService, "kuadrantCtlService", TestDoubles.failingKuadrantCtl());
        ReflectionTestSupport.inject(migrationService, "migrationAgent", (io.gateforge.ai.MigrationAgent) prompt -> "test analysis");
        ReflectionTestSupport.inject(migrationService, "metrics", new GateForgeMetrics(new SimpleMeterRegistry()));
        ReflectionTestSupport.inject(migrationService, "prerequisiteCatalogService", catalogService);
        ReflectionTestSupport.inject(migrationService, "toolConfigPrerequisiteChecker", toolConfigChecker);
        ReflectionTestSupport.inject(migrationService, "clusterReadinessService", readinessService);
        ReflectionTestSupport.inject(migrationService, "gatewayClassName", "istio");
        ReflectionTestSupport.inject(migrationService, "gatewayNamespace", "kuadrant-system");
        ReflectionTestSupport.inject(migrationService, "clusterDomain", "apps.example.com");
        ReflectionTestSupport.inject(migrationService, "developerHubEnabled", false);
        ReflectionTestSupport.inject(migrationService, "observabilityEnabled", false);
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
        assertTrue(plan.prerequisites().stream().anyMatch(p -> "gateforge-cluster-api".equals(p.id())));

        plan.prerequisites().forEach(p -> assertEquals("unknown", p.status(),
                "Expected unknown for " + p.id() + " but was " + p.status()));
    }

    static class MigrationServiceForTest extends MigrationService {
        @Override
        void persistPlan(MigrationPlan plan) {
            // no-op for unit test
        }
    }
}
