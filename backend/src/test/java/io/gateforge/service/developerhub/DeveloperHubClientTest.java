package io.gateforge.service.developerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DeveloperHubClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void isActive_whenEnabledWithValidUrl_returnsTrue() {
        DeveloperHubClient client = new DeveloperHubClient(
                true, "https://developer-hub.example.com", Optional.empty(), Optional.empty(),
                "apps.example.com", "-product", objectMapper);

        assertTrue(client.isActive());
    }

    @Test
    void isActive_whenDisabled_returnsFalse() {
        DeveloperHubClient client = new DeveloperHubClient(
                false, "https://developer-hub.example.com", Optional.empty(), Optional.empty(),
                "apps.example.com", "-product", objectMapper);

        assertFalse(client.isActive());
    }

    @Test
    void isActive_whenUrlIsNone_returnsFalse() {
        DeveloperHubClient client = new DeveloperHubClient(
                true, "none", Optional.empty(), Optional.empty(),
                "apps.example.com", "-product", objectMapper);

        assertFalse(client.isActive());
    }

    @Test
    void normalizeBaseUrl_stripsTrailingSlash() {
        assertEquals("https://hub.example.com", DeveloperHubClient.normalizeBaseUrl("https://hub.example.com/"));
        assertEquals("https://hub.example.com", DeveloperHubClient.normalizeBaseUrl("https://hub.example.com"));
    }

    @Test
    void toSystemName_sanitizesProductName() {
        assertEquals("demo-api", DeveloperHubClient.toSystemName("Demo API"));
        assertEquals("my-product-", DeveloperHubClient.toSystemName("My Product!"));
    }
}
