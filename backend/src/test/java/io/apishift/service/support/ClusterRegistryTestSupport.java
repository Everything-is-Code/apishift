package io.apishift.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.apishift.model.TargetCluster;
import io.apishift.service.ClusterRegistry;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ClusterRegistryTestSupport {

    private ClusterRegistryTestSupport() {}

    public static ClusterRegistry create(KubernetesClient localClient) {
        ClusterRegistry registry = new ClusterRegistry();
        ReflectionTestSupport.inject(registry, "localClient", localClient);
        ReflectionTestSupport.inject(registry, "objectMapper", new ObjectMapper());
        ReflectionTestSupport.inject(registry, "clustersJson", Optional.empty());
        ReflectionTestSupport.inject(registry, "argocdDiscovery", false);
        seedLocal(registry, localClient);
        return registry;
    }

    public static void seedLocal(ClusterRegistry registry, KubernetesClient localClient) {
        Map<String, TargetCluster> clusters = clusterMap(registry);
        Map<String, KubernetesClient> clients = clientMap(registry);
        clusters.put("local", TargetCluster.local());
        clients.put("local", localClient);
    }

    public static void seedCluster(ClusterRegistry registry, TargetCluster cluster, KubernetesClient client) {
        Map<String, TargetCluster> clusters = clusterMap(registry);
        Map<String, KubernetesClient> clients = clientMap(registry);
        clusters.put(cluster.id(), cluster);
        clients.put(cluster.id(), client);
    }

    public static void seedClusterWithoutClient(ClusterRegistry registry, TargetCluster cluster) {
        clusterMap(registry).put(cluster.id(), cluster);
    }

    public static void invokeArgoCDDiscovery(ClusterRegistry registry) {
        try {
            Method method = ClusterRegistry.class.getDeclaredMethod("discoverArgoCDClusters");
            method.setAccessible(true);
            method.invoke(registry);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke ArgoCD discovery", e);
        }
    }

    public static TargetCluster labCluster(String id) {
        return new TargetCluster(
                id,
                "Lab cluster",
                "https://api.lab.example:6443",
                "token",
                "bearer",
                false,
                true);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, TargetCluster> clusterMap(ClusterRegistry registry) {
        try {
            var field = ClusterRegistry.class.getDeclaredField("clusters");
            field.setAccessible(true);
            return (Map<String, TargetCluster>) field.get(registry);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to access clusters map", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, KubernetesClient> clientMap(ClusterRegistry registry) {
        try {
            var field = ClusterRegistry.class.getDeclaredField("clients");
            field.setAccessible(true);
            return (Map<String, KubernetesClient>) field.get(registry);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to access clients map", e);
        }
    }
}
