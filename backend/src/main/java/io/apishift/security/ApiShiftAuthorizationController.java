package io.apishift.security;

import io.quarkus.security.spi.runtime.AuthorizationController;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.Interceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER + 10)
@ApplicationScoped
public class ApiShiftAuthorizationController extends AuthorizationController {

    @ConfigProperty(name = "apishift.auth.enabled", defaultValue = "false")
    boolean authEnabled;

    @Override
    public boolean isAuthorizationEnabled() {
        return authEnabled;
    }
}
