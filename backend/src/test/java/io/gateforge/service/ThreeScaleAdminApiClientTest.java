package io.gateforge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.service.support.ThreeScaleAdminApiFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThreeScaleAdminApiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseServices_extractsServiceMaps() throws Exception {
        JsonNode root = objectMapper.readTree(ThreeScaleAdminApiFixtures.SERVICES_JSON);

        List<Map<String, Object>> result = ThreeScaleAdminApiClient.parsePaginatedList(
                root, "services", "service", objectMapper);

        assertEquals(1, result.size());
        assertEquals("demo-api", result.get(0).get("system_name"));
        assertEquals("Demo API", result.get(0).get("name"));
        assertEquals(1, ((Number) result.get(0).get("id")).intValue());
    }

    @Test
    void parseBackendApis_extractsBackendMaps() throws Exception {
        JsonNode root = objectMapper.readTree(ThreeScaleAdminApiFixtures.BACKEND_APIS_JSON);

        List<Map<String, Object>> result = ThreeScaleAdminApiClient.parsePaginatedList(
                root, "backend_apis", "backend_api", objectMapper);

        assertEquals(1, result.size());
        assertEquals("api-backend", result.get(0).get("system_name"));
        assertEquals("http://api-backend.default.svc:8080", result.get(0).get("private_endpoint"));
    }

    @Test
    void parseEmptyCollection_returnsEmptyList() throws Exception {
        JsonNode root = objectMapper.readTree(ThreeScaleAdminApiFixtures.EMPTY_SERVICES_JSON);

        List<Map<String, Object>> result = ThreeScaleAdminApiClient.parsePaginatedList(
                root, "services", "service", objectMapper);

        assertTrue(result.isEmpty());
    }

    @Test
    void isConfigured_detectsDefaultsAndValidConfig() {
        ThreeScaleAdminApiClient unconfigured = new ThreeScaleAdminApiClient(
                "default", "http://localhost", "none", objectMapper);
        ThreeScaleAdminApiClient configured = new ThreeScaleAdminApiClient(
                "prod", "https://3scale.example.com", "secret-token", objectMapper);

        assertFalse(unconfigured.isConfigured());
        assertTrue(configured.isConfigured());
    }
}
