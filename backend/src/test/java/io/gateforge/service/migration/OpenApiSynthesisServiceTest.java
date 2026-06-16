package io.gateforge.service.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.model.ThreeScaleProduct;
import io.gateforge.service.support.MigrationFixtures;
import io.gateforge.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSynthesisServiceTest {

    OpenApiSynthesisService service;

    @BeforeEach
    void setUp() {
        service = new OpenApiSynthesisService();
        ReflectionTestSupport.inject(service, "objectMapper", new ObjectMapper());
    }

    @Test
    void synthesizeFromMappingRules_withRules_includesPathOperations() throws Exception {
        ThreeScaleProduct product = MigrationFixtures.apiKeyProduct();
        List<ThreeScaleProduct.MappingRule> rules = List.of(
                new ThreeScaleProduct.MappingRule("GET", "/accounts", "hits", 1),
                new ThreeScaleProduct.MappingRule("POST", "/accounts/{id}", "hits", 1)
        );

        String spec = service.synthesizeFromMappingRules(product, rules, "demo.apps.example.com");

        assertNotNull(spec);
        JsonNode root = new ObjectMapper().readTree(spec);
        assertTrue(root.get("paths").has("/accounts"));
        assertTrue(root.get("paths").has("/accounts/{id}"));
        assertTrue(root.get("paths").get("/accounts").has("get"));
    }

    @Test
    void synthesizeFromMappingRules_emptyRules_buildsMinimalRootPath() throws Exception {
        String spec = service.synthesizeFromMappingRules(MigrationFixtures.apiKeyProduct(), List.of(), "demo.apps.example.com");

        assertNotNull(spec);
        JsonNode root = new ObjectMapper().readTree(spec);
        assertTrue(root.get("paths").has("/"));
    }

    @Test
    void synthesizeFromMappingRules_accountPath_embedsAccountExample() throws Exception {
        List<ThreeScaleProduct.MappingRule> rules = List.of(
                new ThreeScaleProduct.MappingRule("GET", "/accounts", "hits", 1)
        );

        String spec = service.synthesizeFromMappingRules(MigrationFixtures.apiKeyProduct(), rules, "demo.apps.example.com");
        JsonNode root = new ObjectMapper().readTree(spec);

        JsonNode example = root.path("paths").path("/accounts").path("get")
                .path("responses").path("200").path("content")
                .path("application/json").path("example");
        assertTrue(example.isArray());
        assertTrue(example.get(0).has("accountId"));
    }
}
