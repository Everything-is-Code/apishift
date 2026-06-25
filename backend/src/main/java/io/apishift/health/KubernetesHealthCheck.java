package io.apishift.health;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.util.Optional;

@Readiness
@ApplicationScoped
public class KubernetesHealthCheck implements HealthCheck {

    @Inject
    KubernetesClient kubernetesClient;

    @ConfigProperty(name = "quarkus.kubernetes-client.api-server-url")
    Optional<String> configuredApiServerUrl;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("kubernetes");
        if (!isKubernetesConfigured()) {
            return builder.up().withData("status", "not configured").build();
        }
        try {
            String version = kubernetesClient.getKubernetesVersion().getGitVersion();
            return builder.up().withData("version", version).build();
        } catch (Exception e) {
            return builder.down().withData("error", e.getMessage()).build();
        }
    }

    private boolean isKubernetesConfigured() {
        if (isInsideKubernetesCluster()) {
            return true;
        }
        return configuredApiServerUrl.filter(url -> !url.isBlank()).isPresent();
    }

    private static boolean isInsideKubernetesCluster() {
        String serviceHost = System.getenv(Config.KUBERNETES_SERVICE_HOST_PROPERTY);
        return serviceHost != null && !serviceHost.isBlank();
    }
}
