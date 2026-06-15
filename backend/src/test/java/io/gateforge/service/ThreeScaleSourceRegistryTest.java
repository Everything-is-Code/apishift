package io.gateforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.model.ThreeScaleSource;
import io.gateforge.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
