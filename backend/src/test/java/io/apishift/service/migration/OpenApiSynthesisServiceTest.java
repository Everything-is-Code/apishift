package io.apishift.service.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.apishift.model.ThreeScaleProduct;
import io.apishift.service.support.MigrationFixtures;
import io.apishift.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void synthesizeFromMappingRules_postPatchDeleteMethods() throws Exception {
        List<ThreeScaleProduct.MappingRule> rules = List.of(
                new ThreeScaleProduct.MappingRule("POST", "/items", "hits", 1),
                new ThreeScaleProduct.MappingRule("PATCH", "/items/{id}", "hits", 1),
                new ThreeScaleProduct.MappingRule("DELETE", "/items/{id}", "hits", 1)
        );

        String spec = service.synthesizeFromMappingRules(MigrationFixtures.apiKeyProduct(), rules, "demo.apps.example.com");
        JsonNode root = new ObjectMapper().readTree(spec);

        assertTrue(root.get("paths").get("/items").has("post"));
        assertTrue(root.get("paths").get("/items/{id}").has("patch"));
        assertTrue(root.get("paths").get("/items/{id}").has("delete"));
    }

    @Test
    void parseExternalYaml_validOpenApi_returnsPathsMap() {
        String yaml = """
                openapi: 3.0.3
                info:
                  title: demo
                  version: 1.0.0
                paths:
                  /items:
                    get:
                      operationId: listItems
                """;

        Map<String, Object> parsed = service.parseExternalYaml(yaml);

        assertNotNull(parsed);
        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) parsed.get("paths");
        assertNotNull(paths);
        assertTrue(paths.containsKey("/items"));
    }

    @Test
    void parseExternalYaml_taggedPayload_doesNotInstantiateObjects() {
        String yaml = "!!java.net.URL [http://evil.example.com]\n";

        assertNull(service.parseExternalYaml(yaml));
    }

    @Test
    void parseExternalYaml_oversizeBody_returnsNull() {
        char[] chars = new char[OpenApiSynthesisService.MAX_EXTERNAL_YAML_CHARS + 1];
        java.util.Arrays.fill(chars, 'a');
        String yaml = "openapi: 3.0.3\ninfo:\n  title: x\npaths: {}\n" + new String(chars);

        assertNull(service.parseExternalYaml(yaml));
    }

    @Test
    void parseExternalYaml_excessiveNesting_returnsNull() {
        StringBuilder yaml = new StringBuilder();
        for (int depth = 0; depth < 25; depth++) {
            yaml.append("  ".repeat(depth)).append("level").append(depth).append(":\n");
        }
        yaml.append("  ".repeat(25)).append("value: 1\n");

        assertNull(service.parseExternalYaml(yaml.toString()));
    }
}
