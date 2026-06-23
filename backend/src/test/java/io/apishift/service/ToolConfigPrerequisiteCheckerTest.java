package io.apishift.service;

import io.apishift.model.MigrationPrerequisite;
import io.apishift.model.TargetCluster;
import io.apishift.service.support.ClusterRegistryStub;
import io.apishift.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolConfigPrerequisiteCheckerTest {

    private ToolConfigPrerequisiteChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ToolConfigPrerequisiteChecker();
    }

    @Test
    void fromConfig_disconnectedCluster_addsClusterApiPrerequisite() {
        ReflectionTestSupport.inject(checker, "clusterRegistry", ClusterRegistryStub.disconnected());

        List<MigrationPrerequisite> result = checker.fromConfig("local");

        assertTrue(result.stream().anyMatch(p ->
                "apishift-cluster-api".equals(p.id())
                        && "tool-config".equals(p.category())
                        && "unknown".equals(p.status())));
    }

    @Test
    void fromConfig_invalidNonLocalTarget_addsTargetClusterPrerequisite() {
        ReflectionTestSupport.inject(checker, "clusterRegistry",
                ClusterRegistryStub.connected(null).withCluster(null));
        ReflectionTestSupport.inject(checker, "clusterDomain", "apps.example.com");

        List<MigrationPrerequisite> result = checker.fromConfig("staging");

        assertTrue(result.stream().anyMatch(p ->
                "apishift-target-cluster".equals(p.id()) && "unknown".equals(p.status())));
    }

    @Test
    void fromConfig_defaultClusterDomain_addsDomainPrerequisite() {
        ReflectionTestSupport.inject(checker, "clusterRegistry", ClusterRegistryStub.connected(null));
        ReflectionTestSupport.inject(checker, "clusterDomain", "apps.cluster.example.com");

        List<MigrationPrerequisite> result = checker.fromConfig("local");

        assertTrue(result.stream().anyMatch(p ->
                "apishift-cluster-domain".equals(p.id()) && "unknown".equals(p.status())));
    }

    @Test
    void fromConfig_configuredLocal_returnsEmpty() {
        ReflectionTestSupport.inject(checker, "clusterRegistry", ClusterRegistryStub.connected(null));
        ReflectionTestSupport.inject(checker, "clusterDomain", "apps.example.com");

        List<MigrationPrerequisite> result = checker.fromConfig("local");

        assertTrue(result.isEmpty());
    }
}
