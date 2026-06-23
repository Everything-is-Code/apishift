package io.gateforge.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    @Inject
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("database");
        try (var conn = dataSource.getConnection()) {
            return builder.up()
                    .withData("database", conn.getMetaData().getDatabaseProductName())
                    .withData("valid", conn.isValid(5))
                    .build();
        } catch (Exception e) {
            return builder.down().withData("error", e.getMessage()).build();
        }
    }
}
