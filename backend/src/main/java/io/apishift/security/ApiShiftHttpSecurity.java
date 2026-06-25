package io.apishift.security;

import io.quarkus.vertx.http.security.HttpSecurity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ApiShiftHttpSecurity {

    void configure(@Observes HttpSecurity httpSecurity,
            @ConfigProperty(name = "apishift.auth.enabled", defaultValue = "false") boolean authEnabled) {
        httpSecurity
                .path("/q/health", "/q/health/*").permit()
                .path("/q/metrics", "/q/metrics/*").permit();

        if (authEnabled) {
            httpSecurity.path("/mcp", "/mcp/*").roles(ApiShiftRoles.ADMIN);
        }
    }
}
