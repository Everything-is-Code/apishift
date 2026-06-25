package io.apishift.health;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KubernetesHealthCheckTest {

    @Test
    void up_whenKubernetesNotConfigured() {
        KubernetesClient client = mock(KubernetesClient.class);

        KubernetesHealthCheck check = new KubernetesHealthCheck();
        setField(check, "kubernetesClient", client);
        setField(check, "configuredApiServerUrl", Optional.empty());

        HealthCheckResponse response = check.call();
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertEquals("not configured", response.getData().get().get("status"));
        verify(client, never()).getKubernetesVersion();
    }

    @Test
    void up_whenClusterReachable() {
        KubernetesClient client = mock(KubernetesClient.class);
        VersionInfo versionInfo = mock(VersionInfo.class);
        when(client.getKubernetesVersion()).thenReturn(versionInfo);
        when(versionInfo.getGitVersion()).thenReturn("v1.30.2");

        KubernetesHealthCheck check = new KubernetesHealthCheck();
        setField(check, "kubernetesClient", client);
        setField(check, "configuredApiServerUrl", Optional.of("https://api.example.com:6443"));

        HealthCheckResponse response = check.call();
        assertTrue(response.getStatus() == HealthCheckResponse.Status.UP);
        assertEquals("kubernetes", response.getName());
        assertEquals("v1.30.2", response.getData().get().get("version"));
    }

    @Test
    void down_whenClusterUnreachable() {
        KubernetesClient client = mock(KubernetesClient.class);
        when(client.getKubernetesVersion()).thenThrow(new RuntimeException("Connection refused"));

        KubernetesHealthCheck check = new KubernetesHealthCheck();
        setField(check, "kubernetesClient", client);
        setField(check, "configuredApiServerUrl", Optional.of("https://api.example.com:6443"));

        HealthCheckResponse response = check.call();
        assertTrue(response.getStatus() == HealthCheckResponse.Status.DOWN);
        assertEquals("Connection refused", response.getData().get().get("error"));
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
