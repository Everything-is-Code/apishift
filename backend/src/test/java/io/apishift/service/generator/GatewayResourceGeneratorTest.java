package io.apishift.service.generator;

import io.apishift.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayResourceGeneratorTest {

    GatewayResourceGenerator generator;

    @BeforeEach
    void setUp() {
        MigrationGeneratorConfig config = new MigrationGeneratorConfig();
        ReflectionTestSupport.inject(config, "gatewayClassName", "istio");
        ReflectionTestSupport.inject(config, "gatewayNamespace", "kuadrant-system");
        ReflectionTestSupport.inject(config, "clusterDomain", "apps.example.com");

        generator = new GatewayResourceGenerator();
        ReflectionTestSupport.inject(generator, "config", config);
    }

    @Test
    void build_sharedGateway_usesConfiguredClassAndNamespace() {
        var resource = generator.build("apishift-shared", "shared");

        assertEquals("Gateway", resource.kind());
        assertEquals("apishift-shared", resource.name());
        assertEquals("kuadrant-system", resource.namespace());
        assertTrue(resource.yaml().contains("gatewayClassName: istio"));
        assertTrue(resource.yaml().contains("apishift.io/type\": \"shared\""));
    }
}
