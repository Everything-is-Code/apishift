package io.apishift.security;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class ReadProtectionAuthorizationFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "apishift.auth.enabled", defaultValue = "false")
    boolean authEnabled;

    @ConfigProperty(name = "apishift.auth.protect-reads", defaultValue = "false")
    boolean protectReads;

    @Inject
    SecurityIdentity identity;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!authEnabled || !protectReads || !"GET".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        if (!isProtectedReadPath(path)) {
            return;
        }

        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        }
        if (!hasReadRole()) {
            throw new ForbiddenException();
        }
    }

    private boolean hasReadRole() {
        for (String role : ApiShiftRoles.READ_ACCESS) {
            if (identity.hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    static boolean isProtectedReadPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.startsWith("/") ? path : "/" + path;

        if (normalized.equals("/api/migration/policy-mapping")) {
            return false;
        }
        if (normalized.startsWith("/api/migration/plans")) {
            return true;
        }
        if (normalized.equals("/api/audit/reports")) {
            return true;
        }
        if (normalized.startsWith("/api/hub/")) {
            return true;
        }
        if (normalized.startsWith("/api/threescale/products")) {
            return true;
        }
        if (normalized.equals("/api/cluster/targets") || normalized.startsWith("/api/cluster/targets/")) {
            return true;
        }
        return false;
    }
}
