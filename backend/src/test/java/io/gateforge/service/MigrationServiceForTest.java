package io.gateforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.model.MigrationPlan;
import io.gateforge.model.ThreeScaleProduct;
import io.gateforge.service.support.ClusterRegistryStub;
import io.gateforge.service.support.ReflectionTestSupport;
import io.gateforge.service.support.TestDoubles;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;

/**
 * Test double that skips database persistence during {@link MigrationService#analyze()}.
 */
class MigrationServiceForTest extends MigrationService {

    static MigrationServiceForTest createWithProducts(List<ThreeScaleProduct> products) {
        return createWithProducts(products, false);
    }

    static MigrationServiceForTest createWithProducts(List<ThreeScaleProduct> products, boolean developerHubEnabled) {
        MigrationServiceForTest migrationService = new MigrationServiceForTest();

        PrerequisiteCatalogService catalogService = new PrerequisiteCatalogService();
        ToolConfigPrerequisiteChecker toolConfigChecker = new ToolConfigPrerequisiteChecker();
        ClusterReadinessService readinessService = new ClusterReadinessService();

        ReflectionTestSupport.inject(toolConfigChecker, "clusterRegistry", ClusterRegistryStub.disconnected());
        ReflectionTestSupport.inject(toolConfigChecker, "clusterDomain", "apps.cluster.example.com");
        ReflectionTestSupport.inject(readinessService, "clusterRegistry", ClusterRegistryStub.disconnected());
        ReflectionTestSupport.inject(readinessService, "catalogService", catalogService);

        ReflectionTestSupport.inject(migrationService, "threeScaleService", TestDoubles.threeScaleService(products));
        ReflectionTestSupport.inject(migrationService, "sourceRegistry", TestDoubles.emptySourceRegistry());
        ReflectionTestSupport.inject(migrationService, "clusterRegistry", ClusterRegistryStub.disconnected());
        ReflectionTestSupport.inject(migrationService, "objectMapper", new ObjectMapper());
        ReflectionTestSupport.inject(migrationService, "kuadrantCtlService", TestDoubles.failingKuadrantCtl());
        ReflectionTestSupport.inject(migrationService, "migrationAgent",
                (io.gateforge.ai.MigrationAgent) prompt -> "test analysis");
        ReflectionTestSupport.inject(migrationService, "metrics", new GateForgeMetrics(new SimpleMeterRegistry()));
        ReflectionTestSupport.inject(migrationService, "prerequisiteCatalogService", catalogService);
        ReflectionTestSupport.inject(migrationService, "toolConfigPrerequisiteChecker", toolConfigChecker);
        ReflectionTestSupport.inject(migrationService, "clusterReadinessService", readinessService);
        ReflectionTestSupport.inject(migrationService, "gatewayClassName", "istio");
        ReflectionTestSupport.inject(migrationService, "gatewayNamespace", "kuadrant-system");
        ReflectionTestSupport.inject(migrationService, "clusterDomain", "apps.example.com");
        ReflectionTestSupport.inject(migrationService, "developerHubEnabled", developerHubEnabled);
        ReflectionTestSupport.inject(migrationService, "developerHubUrl", "none");
        ReflectionTestSupport.inject(migrationService, "componentSuffix", "-product");
        ReflectionTestSupport.inject(migrationService, "observabilityEnabled", false);

        return migrationService;
    }

    @Override
    void persistPlan(MigrationPlan plan) {
        // no-op for unit tests
    }
}
