package io.apishift.service.support;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class APICastDiscoveryFixtures {

    private APICastDiscoveryFixtures() {}

    public static GenericKubernetesResource apiManager(
            String name,
            String namespace,
            Map<String, Object> apicastSpec,
            boolean available) {
        GenericKubernetesResource resource = new GenericKubernetesResource();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        metadata.setNamespace(namespace);
        resource.setMetadata(metadata);

        Map<String, Object> spec = new HashMap<>();
        if (apicastSpec != null) {
            spec.put("apicast", apicastSpec);
        }

        Map<String, Object> additional = new HashMap<>();
        additional.put("spec", spec);
        additional.put("status", Map.of("conditions", List.of(
                Map.of("type", "Available", "status", available ? "True" : "False"))));
        resource.setAdditionalProperties(additional);
        return resource;
    }

    public static GenericKubernetesResource product(String name, String namespace) {
        GenericKubernetesResource resource = new GenericKubernetesResource();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        metadata.setNamespace(namespace);
        resource.setMetadata(metadata);
        return resource;
    }

    public static Pod apicastPod(String apiManagerName, String namespace) {
        Pod pod = new Pod();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName("apicast-" + apiManagerName + "-staging-abc");
        metadata.setNamespace(namespace);
        pod.setMetadata(metadata);
        return pod;
    }

    public static Map<String, Object> richApicastSpec() {
        Map<String, Object> staging = Map.of(
                "replicas", 2,
                "resources", Map.of("requests", Map.of("cpu", "100m", "memory", "128Mi")));
        Map<String, Object> production = Map.of("replicas", 3);
        List<Map<String, Object>> policies = List.of(
                Map.of("name", "policy-a", "secretRef", Map.of("name", "sec-a"), "version", "1.0"));
        return Map.of(
                "stagingSpec", staging,
                "productionSpec", production,
                "customPolicies", policies,
                "tls", Map.of("verify", true, "verifyDepth", 1),
                "openTracing", Map.of("enabled", true, "tracingLibrary", "jaeger"));
    }

    public static PodList podList(Pod... pods) {
        PodList list = new PodList();
        list.setItems(List.of(pods));
        return list;
    }
}
