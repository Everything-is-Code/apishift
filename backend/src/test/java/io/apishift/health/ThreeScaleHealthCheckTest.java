package io.apishift.health;

import io.apishift.port.threescale.ThreeScaleAdminPort;
import io.apishift.service.ThreeScaleSourceRegistry;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ThreeScaleHealthCheckTest {

    @Test
    void up_whenNoSourcesConfigured() {
        ThreeScaleSourceRegistry registry = mock(ThreeScaleSourceRegistry.class);
        when(registry.hasConfiguredClients()).thenReturn(false);

        ThreeScaleHealthCheck check = new ThreeScaleHealthCheck();
        setField(check, "sourceRegistry", registry);

        HealthCheckResponse response = check.call();
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertEquals(0L, response.getData().get().get("sources"));
    }

    @Test
    void up_whenSourceReachable() throws Exception {
        ThreeScaleSourceRegistry registry = mock(ThreeScaleSourceRegistry.class);
        ThreeScaleAdminPort client = mock(ThreeScaleAdminPort.class);
        when(registry.hasConfiguredClients()).thenReturn(true);
        when(registry.getAllClients()).thenReturn(List.of(client));
        when(client.isConfigured()).thenReturn(true);
        doNothing().when(client).ping();

        ThreeScaleHealthCheck check = new ThreeScaleHealthCheck();
        setField(check, "sourceRegistry", registry);

        HealthCheckResponse response = check.call();
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertEquals(1L, response.getData().get().get("sources-total"));
        assertEquals(1L, response.getData().get().get("sources-reachable"));
    }

    @Test
    void down_whenAllSourcesUnreachable() throws Exception {
        ThreeScaleSourceRegistry registry = mock(ThreeScaleSourceRegistry.class);
        ThreeScaleAdminPort client = mock(ThreeScaleAdminPort.class);
        when(registry.hasConfiguredClients()).thenReturn(true);
        when(registry.getAllClients()).thenReturn(List.of(client));
        when(client.isConfigured()).thenReturn(true);
        doThrow(new RuntimeException("timeout")).when(client).ping();

        ThreeScaleHealthCheck check = new ThreeScaleHealthCheck();
        setField(check, "sourceRegistry", registry);

        HealthCheckResponse response = check.call();
        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals(1L, response.getData().get().get("sources-total"));
        assertEquals(0L, response.getData().get().get("sources-reachable"));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
