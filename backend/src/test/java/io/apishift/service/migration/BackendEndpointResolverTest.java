package io.apishift.service.migration;

import io.apishift.port.threescale.ThreeScaleAdminPort;
import io.apishift.service.ThreeScaleSourceRegistry;
import io.apishift.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackendEndpointResolverTest {

    @Mock
    ThreeScaleSourceRegistry sourceRegistry;

    @Mock
    ThreeScaleAdminPort client;

    BackendEndpointResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new BackendEndpointResolver();
        ReflectionTestSupport.inject(resolver, "sourceRegistry", sourceRegistry);
    }

    @Test
    void resolveIndex_noConfiguredClients_returnsEmptyIndex() {
        when(sourceRegistry.hasConfiguredClients()).thenReturn(false);

        BackendEndpointResolver.BackendIndex index = resolver.resolveIndex();

        assertTrue(index.byId().isEmpty());
        assertTrue(index.byName().isEmpty());
    }

    @Test
    void resolveIndex_indexesBackendsByIdSystemNameAndDisplayName() throws Exception {
        when(sourceRegistry.hasConfiguredClients()).thenReturn(true);
        when(sourceRegistry.getAllClients()).thenReturn(List.of(client));
        when(client.isConfigured()).thenReturn(true);
        when(client.listBackendApis()).thenReturn(List.of(
                Map.of(
                        "id", 42L,
                        "system_name", "echo_api",
                        "name", "Echo API",
                        "private_endpoint", "http://echo.echo-ns.svc.cluster.local:8080"
                )
        ));

        BackendEndpointResolver.BackendIndex index = resolver.resolveIndex();

        assertEquals("http://echo.echo-ns.svc.cluster.local:8080", index.byId().get(42L));
        assertEquals("http://echo.echo-ns.svc.cluster.local:8080", index.lookup("echo_api"));
        assertEquals("http://echo.echo-ns.svc.cluster.local:8080", index.lookup("Echo API"));
        assertEquals("http://echo.echo-ns.svc.cluster.local:8080", index.lookup("backend-42"));
    }

    @Test
    void parseServiceName_extractsFirstDnsLabel() {
        assertEquals("echo", BackendEndpointResolver.parseServiceName(
                "http://echo.echo-ns.svc.cluster.local:8080"));
    }

    @Test
    void parseServiceNamespace_extractsSecondDnsLabel() {
        assertEquals("echo-ns", BackendEndpointResolver.parseServiceNamespace(
                "http://echo.echo-ns.svc.cluster.local:8080"));
    }

    @Test
    void parseBackendId_readsNumericSuffix() {
        assertEquals(99L, BackendEndpointResolver.parseBackendId("backend-99"));
        assertEquals(0L, BackendEndpointResolver.parseBackendId("echo_api"));
    }
}
