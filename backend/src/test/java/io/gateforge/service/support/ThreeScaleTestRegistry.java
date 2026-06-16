package io.gateforge.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.model.ThreeScaleSource;
import io.gateforge.port.threescale.ThreeScaleAdminPort;
import io.gateforge.service.ThreeScaleSourceRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ThreeScaleTestRegistry {

    private ThreeScaleTestRegistry() {}

    public static ThreeScaleSourceRegistry withClients(
            Map<String, ThreeScaleAdminPort> clients,
            Map<String, ThreeScaleSource> sources) {
        ThreeScaleSourceRegistry registry = new ThreeScaleSourceRegistry();
        ReflectionTestSupport.inject(registry, "objectMapper", new ObjectMapper());
        ReflectionTestSupport.inject(registry, "clients", new ConcurrentHashMap<>(clients));
        ReflectionTestSupport.inject(registry, "sources", new ConcurrentHashMap<>(sources));
        return registry;
    }

    public static ThreeScaleSourceRegistry withStub(
            String sourceId, String label, StubThreeScaleAdminApiClient client) {
        ThreeScaleSource source = new ThreeScaleSource(
                sourceId, label, "https://3scale.example.com", "test-token", true);
        return withClients(
                Map.of(sourceId, client),
                Map.of(sourceId, source));
    }
}
