package io.gateforge.service;

import io.gateforge.model.MigrationPrerequisite;
import io.gateforge.service.support.ClusterRegistryStub;
import io.gateforge.service.support.KubernetesClientStub;
import io.gateforge.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClusterReadinessServiceTest {

    private ClusterReadinessService service;
    private PrerequisiteCatalogService catalogService;

    @BeforeEach
    void setUp() {
        service = new ClusterReadinessService();
        catalogService = new PrerequisiteCatalogService();
        ReflectionTestSupport.inject(service, "catalogService", catalogService);
    }

    @Test
    void enrich_disconnectedCluster_setsAllUnknown() {
        ReflectionTestSupport.inject(service, "clusterRegistry", ClusterRegistryStub.disconnected());
        List<MigrationPrerequisite> input = List.of(
                catalogService.definitionForId("gateway-api", 1, "unknown"),
                catalogService.definitionForId("rhcl-core", 1, "unknown"));

        List<MigrationPrerequisite> result = service.enrich(input, "local");

        assertEquals(2, result.size());
        result.forEach(p -> assertEquals("unknown", p.status()));
    }

    @Test
    void enrich_rbacDenied_mapsToUnknownNotMissing() {
        ReflectionTestSupport.inject(service, "clusterRegistry",
                ClusterRegistryStub.connected(KubernetesClientStub.create(KubernetesClientStub.Mode.FORBIDDEN)));
        List<MigrationPrerequisite> input = List.of(
                catalogService.definitionForId("gateway-api", 1, "unknown"));

        List<MigrationPrerequisite> result = service.enrich(input, "local");

        assertEquals(1, result.size());
        assertEquals("unknown", result.get(0).status());
    }

    @Test
    void enrich_absentCrd_mapsToMissing() {
        ReflectionTestSupport.inject(service, "clusterRegistry",
                ClusterRegistryStub.connected(KubernetesClientStub.create(KubernetesClientStub.Mode.MISSING)));
        List<MigrationPrerequisite> input = List.of(
                catalogService.definitionForId("gateway-api", 1, "unknown"));

        List<MigrationPrerequisite> result = service.enrich(input, "local");

        assertEquals(1, result.size());
        assertEquals("missing", result.get(0).status());
    }

    @Test
    void enrich_presentCrd_mapsToSatisfied() {
        ReflectionTestSupport.inject(service, "clusterRegistry",
                ClusterRegistryStub.connected(KubernetesClientStub.create(KubernetesClientStub.Mode.SATISFIED)));
        List<MigrationPrerequisite> input = List.of(
                catalogService.definitionForId("gateway-api", 1, "unknown"));

        List<MigrationPrerequisite> result = service.enrich(input, "local");

        assertEquals(1, result.size());
        assertEquals("satisfied", result.get(0).status());
    }
}
