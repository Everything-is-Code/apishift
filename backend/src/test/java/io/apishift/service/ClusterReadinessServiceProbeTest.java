package io.apishift.service;

import io.apishift.model.ClusterReadiness;
import io.apishift.model.MigrationPrerequisite;
import io.apishift.repository.MigrationPlanRepository;
import io.apishift.service.support.ClusterRegistryStub;
import io.apishift.service.support.KubernetesClientStub;
import io.apishift.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClusterReadinessServiceProbeTest {

    private ClusterReadinessService service;
    private PrerequisiteCatalogService catalogService;
    private MigrationPlanRepository migrationPlanRepository;

    @BeforeEach
    void setUp() {
        service = new ClusterReadinessService();
        catalogService = new PrerequisiteCatalogService();
        migrationPlanRepository = mock(MigrationPlanRepository.class);
        ReflectionTestSupport.inject(service, "catalogService", catalogService);
        ReflectionTestSupport.inject(service, "migrationPlanRepository", migrationPlanRepository);
    }

    @Test
    void enrich_emptyList_returnsEmpty() {
        assertTrue(service.enrich(List.of(), "local").isEmpty());
        assertTrue(service.enrich(null, "local").isEmpty());
    }

    @Test
    void probe_disconnectedCluster_reportsUnknownConnection() {
        ReflectionTestSupport.inject(service, "clusterRegistry", ClusterRegistryStub.disconnected());

        ClusterReadiness readiness = service.probe("local", null);

        assertFalse(readiness.clusterConnected());
        assertEquals("unknown", readiness.connectionStatus());
        assertFalse(readiness.prerequisites().isEmpty());
    }

    @Test
    void probe_connectedCluster_enrichesBaselinePrerequisites() {
        ReflectionTestSupport.inject(service, "clusterRegistry",
                ClusterRegistryStub.connected(KubernetesClientStub.create(KubernetesClientStub.Mode.SATISFIED)));

        ClusterReadiness readiness = service.probe("local", null);

        assertTrue(readiness.clusterConnected());
        assertEquals("satisfied", readiness.connectionStatus());
        assertTrue(readiness.prerequisites().stream()
                .anyMatch(p -> "gateway-api".equals(p.id()) && "satisfied".equals(p.status())));
    }

    @Test
    void probe_withStoredPrerequisites_usesRepositorySnapshot() {
        ReflectionTestSupport.inject(service, "clusterRegistry",
                ClusterRegistryStub.connected(KubernetesClientStub.create(KubernetesClientStub.Mode.SATISFIED)));
        List<MigrationPrerequisite> stored = List.of(
                catalogService.definitionForId("gateway-api", 1, "unknown"),
                catalogService.definitionForId("rhcl-core", 1, "unknown"),
                catalogService.definitionForId("authorino-secrets", 1, "unknown"));
        when(migrationPlanRepository.findPrerequisites("plan-1")).thenReturn(Optional.of(stored));

        ClusterReadiness readiness = service.probe("local", "plan-1");

        assertTrue(readiness.clusterConnected());
        assertEquals(3, readiness.prerequisites().size());
        assertEquals("satisfied", readiness.prerequisites().stream()
                .filter(p -> "authorino-secrets".equals(p.id()))
                .findFirst()
                .orElseThrow()
                .status());
    }
}
