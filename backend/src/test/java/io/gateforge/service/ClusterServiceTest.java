package io.gateforge.service;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.gateforge.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClusterServiceTest {

    @Test
    void getProject_existingNamespace_returnsProjectInfo() {
        Namespace ns = namespace("demo-ns", "Active");
        KubernetesClient client = kubernetesClient(ns);
        ThreeScaleService threeScaleService = mock(ThreeScaleService.class);
        when(threeScaleService.listProducts()).thenReturn(List.of());

        ClusterService service = new ClusterService();
        ReflectionTestSupport.inject(service, "kubernetesClient", client);
        ReflectionTestSupport.inject(service, "threeScaleService", threeScaleService);

        var project = service.getProject("demo-ns");

        assertEquals("demo-ns", project.name());
        assertEquals("Active", project.status());
    }

    @Test
    void getProject_missingNamespace_returnsNull() {
        KubernetesClient client = kubernetesClient();
        ThreeScaleService threeScaleService = mock(ThreeScaleService.class);

        ClusterService service = new ClusterService();
        ReflectionTestSupport.inject(service, "kubernetesClient", client);
        ReflectionTestSupport.inject(service, "threeScaleService", threeScaleService);

        assertNull(service.getProject("missing"));
    }

    @SuppressWarnings("unchecked")
    private static KubernetesClient kubernetesClient(Namespace... namespaces) {
        NamespaceList list = new NamespaceList();
        list.setItems(List.of(namespaces));

        Resource<Namespace> namespaceResource = mock(Resource.class);
        when(namespaceResource.get()).thenAnswer(inv -> {
            String name = inv.getMock().toString();
            for (Namespace ns : namespaces) {
                if (ns.getMetadata().getName().equals(name)) {
                    return ns;
                }
            }
            return null;
        });

        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespaceOp =
                mock(NonNamespaceOperation.class);
        when(namespaceOp.list()).thenReturn(list);
        when(namespaceOp.withName(anyString())).thenAnswer(inv -> {
            String requested = inv.getArgument(0);
            Resource<Namespace> resource = mock(Resource.class);
            for (Namespace ns : namespaces) {
                if (ns.getMetadata().getName().equals(requested)) {
                    when(resource.get()).thenReturn(ns);
                    return resource;
                }
            }
            when(resource.get()).thenReturn(null);
            return resource;
        });

        AnyNamespaceOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>>
                inAnyNamespace = mock(AnyNamespaceOperation.class);
        when(inAnyNamespace.list()).thenReturn(new GenericKubernetesResourceList());

        MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>>
                genericOp = mock(MixedOperation.class);
        when(genericOp.inAnyNamespace()).thenReturn(inAnyNamespace);

        KubernetesClient client = mock(KubernetesClient.class);
        when(client.namespaces()).thenReturn(namespaceOp);
        when(client.genericKubernetesResources(any())).thenReturn(genericOp);
        return client;
    }

    private static Namespace namespace(String name, String phase) {
        return new NamespaceBuilder()
                .withNewMetadata()
                .withName(name)
                .withCreationTimestamp("2026-01-01T00:00:00Z")
                .endMetadata()
                .withNewStatus()
                .withPhase(phase)
                .endStatus()
                .build();
    }
}
