package io.apishift.service.support;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mockito-based Kubernetes client for {@code @QuarkusTest} migration lifecycle tests.
 */
public final class MigrationKubernetesTestSupport {

    private MigrationKubernetesTestSupport() {}

    public static KubernetesClient inSyncClient() {
        return client(DriftMode.IN_SYNC);
    }

    public static KubernetesClient missingClient() {
        return client(DriftMode.MISSING);
    }

    private enum DriftMode {
        IN_SYNC,
        MISSING
    }

    @SuppressWarnings("unchecked")
    private static KubernetesClient client(DriftMode driftMode) {
        NamespaceableResource<GenericKubernetesResource> resource = mock(NamespaceableResource.class);
        when(resource.serverSideApply()).thenReturn(sampleResource());
        when(resource.createOr(any())).thenReturn(sampleResource());
        when(resource.delete()).thenReturn(java.util.List.of());

        switch (driftMode) {
            case IN_SYNC -> when(resource.get()).thenReturn(sampleResource());
            case MISSING -> when(resource.get()).thenThrow(new RuntimeException("NotFound: resource missing"));
        }

        KubernetesClient kubernetesClient = mock(KubernetesClient.class);
        when(kubernetesClient.resource(any(GenericKubernetesResource.class))).thenReturn(resource);
        return kubernetesClient;
    }

    private static GenericKubernetesResource sampleResource() {
        GenericKubernetesResource generic = new GenericKubernetesResource();
        generic.setMetadata(new ObjectMeta());
        generic.getMetadata().setName("ApiShift-test");
        return generic;
    }
}
