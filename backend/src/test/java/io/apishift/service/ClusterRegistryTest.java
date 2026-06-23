package io.apishift.service;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.apishift.model.TargetCluster;
import io.apishift.service.support.ClusterRegistryKubernetesStub;
import io.apishift.service.support.ClusterRegistryTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClusterRegistryTest {

    @Test
    void getClient_local_returnsLocalClient() {
        KubernetesClient localClient = ClusterRegistryKubernetesStub.create(
                ClusterRegistryKubernetesStub.Options.fullAccess());
        ClusterRegistry registry = ClusterRegistryTestSupport.create(localClient);

        assertSame(localClient, registry.getClient("local"));
    }

    @Test
    void getClient_blank_returnsLocalClient() {
        KubernetesClient localClient = ClusterRegistryKubernetesStub.create(
                ClusterRegistryKubernetesStub.Options.fullAccess());
        ClusterRegistry registry = ClusterRegistryTestSupport.create(localClient);

        assertSame(localClient, registry.getClient(""));
        assertSame(localClient, registry.getClient(null));
    }

    @Test
    void getClient_unknown_fallsBackToLocal() {
        KubernetesClient localClient = ClusterRegistryKubernetesStub.create(
                ClusterRegistryKubernetesStub.Options.fullAccess());
        ClusterRegistry registry = ClusterRegistryTestSupport.create(localClient);

        assertSame(localClient, registry.getClient("missing-cluster"));
    }

    @Test
    void addCluster_registersMetadata() {
        KubernetesClient localClient = ClusterRegistryKubernetesStub.create(
                ClusterRegistryKubernetesStub.Options.fullAccess());
        ClusterRegistry registry = ClusterRegistryTestSupport.create(localClient);
        TargetCluster lab = ClusterRegistryTestSupport.labCluster("lab");

        registry.addCluster(lab);

        assertEquals(lab, registry.getCluster("lab"));
        assertTrue(registry.listClusters().stream().anyMatch(c -> "lab".equals(c.id())));
    }

    @Test
    void removeCluster_nonLocal_removes() {
        KubernetesClient localClient = ClusterRegistryKubernetesStub.create(
                ClusterRegistryKubernetesStub.Options.fullAccess());
        KubernetesClient labClient = ClusterRegistryKubernetesStub.create(
                ClusterRegistryKubernetesStub.Options.fullAccess());
        ClusterRegistry registry = ClusterRegistryTestSupport.create(localClient);
        TargetCluster lab = ClusterRegistryTestSupport.labCluster("lab");
        ClusterRegistryTestSupport.seedCluster(registry, lab, labClient);

        registry.removeCluster("lab");

        assertNull(registry.getCluster("lab"));
        assertFalse(registry.listClusters().stream().anyMatch(c -> "lab".equals(c.id())));
    }

    @Test
    void removeCluster_local_protected() {
        KubernetesClient localClient = ClusterRegistryKubernetesStub.create(
                ClusterRegistryKubernetesStub.Options.fullAccess());
        ClusterRegistry registry = ClusterRegistryTestSupport.create(localClient);

        registry.removeCluster("local");

        assertNotNull(registry.getCluster("local"));
    }

    @Test
    void validateAccess_noClient_returnsError() {
        KubernetesClient localClient = ClusterRegistryKubernetesStub.create(
                ClusterRegistryKubernetesStub.Options.fullAccess());
        ClusterRegistry registry = ClusterRegistryTestSupport.create(localClient);
        TargetCluster lab = ClusterRegistryTestSupport.labCluster("lab");
        ClusterRegistryTestSupport.seedClusterWithoutClient(registry, lab);

        Map<String, Object> result = registry.validateAccess("lab");

        assertEquals(false, result.get("connected"));
        assertTrue(String.valueOf(result.get("error")).contains("No client registered"));
        assertEquals("lab", result.get("clusterId"));
        assertEquals("Lab cluster", result.get("label"));
    }

    @Test
    void validateAccess_namespaceFails_disconnected() {
        KubernetesClient failingClient = ClusterRegistryKubernetesStub.create(new ClusterRegistryKubernetesStub.Options(
                ClusterRegistryKubernetesStub.NamespaceBehavior.FAIL,
                ClusterRegistryKubernetesStub.GatewayBehavior.OK,
                List.of()));
        ClusterRegistry registry = ClusterRegistryTestSupport.create(failingClient);
        TargetCluster lab = ClusterRegistryTestSupport.labCluster("lab");
        ClusterRegistryTestSupport.seedCluster(registry, lab, failingClient);

        Map<String, Object> result = registry.validateAccess("lab");

        assertEquals(false, result.get("connected"));
        assertNotNull(result.get("error"));
        assertNull(result.get("canManageGateways"));
    }

    @Test
    void validateAccess_gatewaysForbidden_partialAccess() {
        KubernetesClient partialClient = ClusterRegistryKubernetesStub.create(new ClusterRegistryKubernetesStub.Options(
                ClusterRegistryKubernetesStub.NamespaceBehavior.OK,
                ClusterRegistryKubernetesStub.GatewayBehavior.FAIL,
                List.of()));
        ClusterRegistry registry = ClusterRegistryTestSupport.create(partialClient);
        TargetCluster lab = ClusterRegistryTestSupport.labCluster("lab");
        ClusterRegistryTestSupport.seedCluster(registry, lab, partialClient);

        Map<String, Object> result = registry.validateAccess("lab");

        assertEquals(true, result.get("connected"));
        assertEquals(true, result.get("canListNamespaces"));
        assertEquals(false, result.get("canManageGateways"));
    }

    @Test
    void validateAccess_fullAccess() {
        KubernetesClient fullClient = ClusterRegistryKubernetesStub.create(
                ClusterRegistryKubernetesStub.Options.fullAccess());
        ClusterRegistry registry = ClusterRegistryTestSupport.create(fullClient);
        TargetCluster lab = ClusterRegistryTestSupport.labCluster("lab");
        ClusterRegistryTestSupport.seedCluster(registry, lab, fullClient);

        Map<String, Object> result = registry.validateAccess("lab");

        assertEquals(true, result.get("connected"));
        assertEquals(true, result.get("canListNamespaces"));
        assertEquals(true, result.get("canManageGateways"));
    }

    @Test
    void discoverArgoCD_registersExternalCluster() {
        Secret secret = ClusterRegistryKubernetesStub.argocdClusterSecret(
                "lab", "https://api.remote.example:6443", "remote-token");
        KubernetesClient localClient = ClusterRegistryKubernetesStub.create(new ClusterRegistryKubernetesStub.Options(
                ClusterRegistryKubernetesStub.NamespaceBehavior.OK,
                ClusterRegistryKubernetesStub.GatewayBehavior.OK,
                List.of(secret)));
        ClusterRegistry registry = ClusterRegistryTestSupport.create(localClient);

        ClusterRegistryTestSupport.invokeArgoCDDiscovery(registry);

        TargetCluster discovered = registry.getCluster("argocd-lab");
        assertNotNull(discovered);
        assertEquals("https://api.remote.example:6443", discovered.apiServerUrl());
        assertTrue(discovered.label().contains("ArgoCD"));
    }

    @Test
    void discoverArgoCD_skipsKubernetesDefault() {
        Secret secret = ClusterRegistryKubernetesStub.argocdClusterSecret(
                "in-cluster", "https://kubernetes.default.svc", "token");
        KubernetesClient localClient = ClusterRegistryKubernetesStub.create(new ClusterRegistryKubernetesStub.Options(
                ClusterRegistryKubernetesStub.NamespaceBehavior.OK,
                ClusterRegistryKubernetesStub.GatewayBehavior.OK,
                List.of(secret)));
        ClusterRegistry registry = ClusterRegistryTestSupport.create(localClient);

        ClusterRegistryTestSupport.invokeArgoCDDiscovery(registry);

        assertNull(registry.getCluster("argocd-in-cluster"));
        assertEquals(1, registry.listClusters().size());
    }

    @Test
    void discoverArgoCD_malformedConfig_registersClusterWithEmptyToken() {
        Secret secret = ClusterRegistryKubernetesStub.argocdClusterSecretWithConfig(
                "lab", "https://api.remote.example:6443", "{not-json");
        KubernetesClient localClient = ClusterRegistryKubernetesStub.create(new ClusterRegistryKubernetesStub.Options(
                ClusterRegistryKubernetesStub.NamespaceBehavior.OK,
                ClusterRegistryKubernetesStub.GatewayBehavior.OK,
                List.of(secret)));
        ClusterRegistry registry = ClusterRegistryTestSupport.create(localClient);

        ClusterRegistryTestSupport.invokeArgoCDDiscovery(registry);

        TargetCluster discovered = registry.getCluster("argocd-lab");
        assertNotNull(discovered);
        assertEquals("https://api.remote.example:6443", discovered.apiServerUrl());
        assertEquals("", discovered.token());
    }
}
