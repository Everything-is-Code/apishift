package io.gateforge.service.generator;

import io.gateforge.service.support.MigrationFixtures;
import io.gateforge.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRouteResourceGeneratorTest {

    HttpRouteResourceGenerator generator;

    @BeforeEach
    void setUp() {
        MigrationGeneratorConfig config = new MigrationGeneratorConfig();
        ReflectionTestSupport.inject(config, "gatewayClassName", "istio");
        ReflectionTestSupport.inject(config, "gatewayNamespace", "kuadrant-system");
        ReflectionTestSupport.inject(config, "clusterDomain", "apps.example.com");

        generator = new HttpRouteResourceGenerator();
        ReflectionTestSupport.inject(generator, "config", config);
    }

    @Test
    void build_singleBackend_usesPathPrefixRules() {
        var product = MigrationFixtures.apiKeyProduct();

        var resource = generator.build(
                "demo-api-route",
                "default",
                "gateforge-shared",
                product,
                product.mappingRules(),
                "api-backend",
                List.of());

        assertEquals("HTTPRoute", resource.kind());
        assertTrue(resource.yaml().contains("parentRefs:"));
        assertTrue(resource.yaml().contains("name: gateforge-shared"));
        assertTrue(resource.yaml().contains("name: api-backend"));
        assertTrue(resource.yaml().contains("hostnames:"));
        assertTrue(resource.yaml().contains("demo-api.apps.example.com"));
    }

    @Test
    void build_multipleRules_emitsDistinctMatches() {
        var product = MigrationFixtures.apiKeyProduct();
        var rules = List.of(
                new io.gateforge.model.ThreeScaleProduct.MappingRule("GET", "/health", "hits", 1),
                new io.gateforge.model.ThreeScaleProduct.MappingRule("POST", "/orders", "hits", 1));

        var resource = generator.build(
                "demo-api-route",
                "default",
                "gateforge-shared",
                product,
                rules,
                "api-backend",
                List.of());

        assertTrue(resource.yaml().contains("/health"));
        assertTrue(resource.yaml().contains("/orders"));
    }

    @Test
    void build_multipleResolvedBackends_emitsPerBackendRules() {
        var product = MigrationFixtures.apiKeyProduct();
        var resolved = List.of(
                new ResolvedBackend("billing", "default", "/billing"),
                new ResolvedBackend("payments", "default", "/payments"));

        var resource = generator.build(
                "demo-api-route",
                "default",
                "gateforge-shared",
                product,
                List.of(),
                "api-backend",
                resolved);

        assertTrue(resource.yaml().contains("name: billing"));
        assertTrue(resource.yaml().contains("name: payments"));
    }
}
