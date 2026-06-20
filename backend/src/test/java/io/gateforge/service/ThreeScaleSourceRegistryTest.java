package io.gateforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.model.ThreeScaleSource;
import io.gateforge.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThreeScaleSourceRegistryTest {

    private ThreeScaleSourceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ThreeScaleSourceRegistry();
        ReflectionTestSupport.inject(registry, "objectMapper", new ObjectMapper());
    }

    @Test
    void addSource_registersClientAndSource() {
        ThreeScaleSource source = new ThreeScaleSource(
                "lab", "Lab 3scale", "https://3scale.example.com", "token", true);

        registry.addSource(source);

        assertEquals(1, registry.listSources().size());
        assertNotNull(registry.getClient("lab"));
        assertEquals("Lab 3scale", registry.getSource("lab").label());
    }

    @Test
    void removeSource_dropsClient() {
        ThreeScaleSource source = new ThreeScaleSource(
                "lab", "Lab 3scale", "https://3scale.example.com", "token", true);
        registry.addSource(source);

        registry.removeSource("lab");

        assertTrue(registry.listSources().isEmpty());
        assertNull(registry.getClient("lab"));
    }

    @Test
    void hasConfiguredClients_whenMixedSources() {
        registry.addSource(new ThreeScaleSource(
                "local", "Local", "http://localhost", "none", true));
        assertFalse(registry.hasConfiguredClients());

        registry.addSource(new ThreeScaleSource(
                "remote", "Remote", "https://3scale.example.com", "token", true));
        assertTrue(registry.hasConfiguredClients());
    }

    @Test
    void getSourceStatus_unknownSource_returnsError() {
        Map<String, Object> status = registry.getSourceStatus("missing");

        assertEquals("Source not found", status.get("error"));
    }

    @Test
    void getAllClients_returnsRegisteredClients() {
        registry.addSource(new ThreeScaleSource(
                "lab", "Lab 3scale", "https://3scale.example.com", "token", true));

        assertEquals(1, registry.getAllClients().size());
        assertNotNull(registry.getDefaultClient());
    }

    @Test
    void getSourceStatus_unreachableAdminApi_marksUnreachable() {
        registry.addSource(new ThreeScaleSource(
                "offline", "Offline", "http://127.0.0.1:1", "token", true));

        Map<String, Object> status = registry.getSourceStatus("offline");

        assertTrue((Boolean) status.get("configured"));
        assertFalse((Boolean) status.get("reachable"));
    }
}
