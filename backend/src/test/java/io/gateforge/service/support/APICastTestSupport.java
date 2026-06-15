package io.gateforge.service.support;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.gateforge.service.APICastDiscoveryService;
import io.gateforge.service.APICastToIstioMapper;

public final class APICastTestSupport {

    private APICastTestSupport() {}

    public static APICastToIstioMapper createMapper() {
        APICastToIstioMapper mapper = new APICastToIstioMapper();
        ReflectionTestSupport.inject(mapper, "clusterDomain", "apps.example.com");
        ReflectionTestSupport.inject(mapper, "gatewayClassName", "istio");
        return mapper;
    }

    public static APICastDiscoveryService createDiscovery(KubernetesClient client, boolean discoveryEnabled) {
        APICastDiscoveryService service = new APICastDiscoveryService();
        ReflectionTestSupport.inject(service, "kubernetesClient", client);
        ReflectionTestSupport.inject(service, "discoveryEnabled", discoveryEnabled);
        return service;
    }
}
