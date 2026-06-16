package io.gateforge.service.migration;

import io.gateforge.port.threescale.ThreeScaleAdminPort;
import io.gateforge.service.ThreeScaleSourceRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves 3scale backend private endpoints from configured Admin API sources.
 */
@ApplicationScoped
public class BackendEndpointResolver {

    private static final Logger LOG = Logger.getLogger(BackendEndpointResolver.class);

    @Inject
    ThreeScaleSourceRegistry sourceRegistry;

    public record BackendIndex(Map<Long, String> byId, Map<String, String> byName) {

        public String lookup(String backendName) {
            long backendId = parseBackendId(backendName);
            if (backendId > 0) {
                String byIdEndpoint = byId.get(backendId);
                if (byIdEndpoint != null) {
                    return byIdEndpoint;
                }
            }
            return byName.get(backendName);
        }
    }

    public BackendIndex resolveIndex() {
        Map<Long, String> byId = new HashMap<>();
        Map<String, String> byName = new HashMap<>();
        if (!sourceRegistry.hasConfiguredClients()) {
            return new BackendIndex(byId, byName);
        }

        for (ThreeScaleAdminPort client : sourceRegistry.getAllClients()) {
            if (!client.isConfigured()) {
                continue;
            }
            try {
                for (Map<String, Object> backend : client.listBackendApis()) {
                    String endpoint = String.valueOf(backend.getOrDefault("private_endpoint", ""));
                    if (endpoint.isBlank()) {
                        continue;
                    }

                    Object idObj = backend.get("id");
                    long id = idObj instanceof Number number ? number.longValue() : 0L;
                    if (id > 0) {
                        byId.put(id, endpoint);
                    }

                    String systemName = String.valueOf(backend.getOrDefault("system_name", ""));
                    if (!systemName.isBlank()) {
                        byName.put(systemName, endpoint);
                    }

                    String name = String.valueOf(backend.getOrDefault("name", ""));
                    if (!name.isBlank()) {
                        byName.put(name, endpoint);
                    }
                }
            } catch (Exception e) {
                LOG.warnf("Failed to resolve backend endpoints for source %s", client.getSourceId());
            }
        }
        return new BackendIndex(byId, byName);
    }

    public static String parseServiceName(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            if (host != null && host.contains(".")) {
                return host.substring(0, host.indexOf('.'));
            }
            return host != null ? host : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String parseServiceNamespace(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            if (host != null) {
                String[] parts = host.split("\\.");
                if (parts.length >= 2) {
                    return parts[1];
                }
            }
        } catch (Exception e) {
            LOG.debugf("Failed to extract namespace from %s", endpoint);
        }
        return null;
    }

    public static long parseBackendId(String backendName) {
        if (backendName != null && backendName.startsWith("backend-")) {
            try {
                return Long.parseLong(backendName.substring("backend-".length()));
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
}
