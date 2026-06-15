package io.gateforge.service.support;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.gateforge.model.TargetCluster;
import io.gateforge.service.ClusterRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClusterRegistryStub extends ClusterRegistry {

    private Map<String, Object> access = Map.of("connected", false);
    private KubernetesClient client;
    private TargetCluster cluster;

    public ClusterRegistryStub withAccess(Map<String, Object> access) {
        this.access = new LinkedHashMap<>(access);
        return this;
    }

    public ClusterRegistryStub withClient(KubernetesClient client) {
        this.client = client;
        return this;
    }

    public ClusterRegistryStub withCluster(TargetCluster cluster) {
        this.cluster = cluster;
        return this;
    }

    public static ClusterRegistryStub disconnected() {
        return new ClusterRegistryStub().withAccess(Map.of("connected", false));
    }

    public static ClusterRegistryStub connected(KubernetesClient client) {
        return new ClusterRegistryStub()
                .withAccess(Map.of("connected", true))
                .withClient(client)
                .withCluster(TargetCluster.local());
    }

    @Override
    public Map<String, Object> validateAccess(String clusterId) {
        Map<String, Object> result = new LinkedHashMap<>(access);
        result.putIfAbsent("clusterId", clusterId);
        return result;
    }

    @Override
    public KubernetesClient getClient(String clusterId) {
        return client;
    }

    @Override
    public TargetCluster getCluster(String id) {
        return cluster;
    }
}
