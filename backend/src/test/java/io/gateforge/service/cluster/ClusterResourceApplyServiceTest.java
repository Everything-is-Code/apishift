package io.gateforge.service.cluster;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.gateforge.model.DriftEntry;
import io.gateforge.model.MigrationPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClusterResourceApplyServiceTest {

    @Mock
    KubernetesClient client;

    @Mock
    NamespaceableResource<GenericKubernetesResource> resource;

    ClusterResourceApplyService service;

    @BeforeEach
    void setUp() {
        service = new ClusterResourceApplyService();
        when(client.resource(any(GenericKubernetesResource.class))).thenReturn(resource);
    }

    @Test
    void checkDrift_resourcePresent_reportsInSync() {
        when(resource.get()).thenReturn(sampleResource());

        DriftEntry entry = service.checkDrift(client, generatedResource());

        assertEquals("in-sync", entry.status());
        assertEquals("HTTPRoute", entry.kind());
    }

    @Test
    void checkDrift_resourceMissing_reportsMissing() {
        when(resource.get()).thenThrow(new RuntimeException("NotFound: routes \"demo\""));

        DriftEntry entry = service.checkDrift(client, generatedResource());

        assertEquals("missing", entry.status());
    }

    @Test
    void applyYaml_openshiftRoute_usesCreateOrUpdate() {
        String yaml = """
                apiVersion: route.openshift.io/v1
                kind: Route
                metadata:
                  name: demo-route
                  namespace: default
                spec:
                  to:
                    kind: Service
                    name: demo
                """;

        when(resource.createOr(any())).thenReturn(sampleResource());

        service.applyYaml(client, yaml, "default");

        verify(resource).createOr(any());
        verify(resource, never()).serverSideApply();
    }

    @Test
    void applyYaml_kuadrantResource_usesServerSideApply() {
        String yaml = """
                apiVersion: gateway.networking.k8s.io/v1
                kind: HTTPRoute
                metadata:
                  name: demo
                  namespace: default
                spec:
                  parentRefs: []
                """;

        when(resource.serverSideApply()).thenReturn(sampleResource());

        service.applyYaml(client, yaml, "default");

        verify(resource).serverSideApply();
        verify(resource, never()).createOr(any());
    }

    @Test
    void deleteResource_unmarshalsAndDeletes() {
        service.deleteResource(client, generatedResource().yaml(), "default");

        ArgumentCaptor<GenericKubernetesResource> captor = ArgumentCaptor.forClass(GenericKubernetesResource.class);
        verify(client).resource(captor.capture());
        assertEquals("demo-route", captor.getValue().getMetadata().getName());
        verify(resource).delete();
    }

    private static MigrationPlan.GeneratedResource generatedResource() {
        return new MigrationPlan.GeneratedResource(
                "HTTPRoute",
                "demo-route",
                "default",
                """
                apiVersion: gateway.networking.k8s.io/v1
                kind: HTTPRoute
                metadata:
                  name: demo-route
                  namespace: default
                spec:
                  parentRefs: []
                """
        );
    }

    private static GenericKubernetesResource sampleResource() {
        GenericKubernetesResource generic = new GenericKubernetesResource();
        generic.setMetadata(new ObjectMeta());
        generic.getMetadata().setName("demo-route");
        return generic;
    }
}
