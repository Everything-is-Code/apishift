package io.apishift.service.support;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.apishift.service.ThreeScaleService;
import io.apishift.service.ThreeScaleSourceRegistry;
import org.infinispan.client.hotrod.RemoteCacheManager;

public abstract class ThreeScaleServiceTestSupport {

    protected ThreeScaleService createService(
            ThreeScaleSourceRegistry registry, RemoteCacheStub cacheStub) {
        ThreeScaleService service = new ThreeScaleService();
        RemoteCacheManager cacheManager = cacheStub.manager();
        KubernetesClient kubernetesClient =
                KubernetesClientStub.create(KubernetesClientStub.Mode.MISSING);

        ReflectionTestSupport.inject(service, "cacheManager", cacheManager);
        ReflectionTestSupport.inject(service, "sourceRegistry", registry);
        ReflectionTestSupport.inject(service, "kubernetesClient", kubernetesClient);
        ReflectionTestSupport.inject(service, "crdDiscoveryEnabled", false);
        ReflectionTestSupport.inject(service, "cacheTtlSeconds", 3600L);
        return service;
    }
}
