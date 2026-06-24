package io.apishift.health;

import io.apishift.service.ThreeScaleSourceRegistry;
import io.apishift.port.threescale.ThreeScaleAdminPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class ThreeScaleHealthCheck implements HealthCheck {

    @Inject
    ThreeScaleSourceRegistry sourceRegistry;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("3scale");

        if (!sourceRegistry.hasConfiguredClients()) {
            return builder.up().withData("sources", 0).withData("status", "no sources configured").build();
        }

        int total = 0;
        int reachable = 0;
        for (ThreeScaleAdminPort client : sourceRegistry.getAllClients()) {
            if (!client.isConfigured()) {
                continue;
            }
            total++;
            try {
                client.ping();
                reachable++;
            } catch (Exception e) {
                builder.withData("error-" + total, e.getMessage());
            }
        }

        builder.withData("sources-total", total).withData("sources-reachable", reachable);
        return reachable > 0 || total == 0 ? builder.up().build() : builder.down().build();
    }
}
