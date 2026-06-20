package io.gateforge.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GateForgeMetricsTest {

    @Test
    void recordMigration_incrementsCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        GateForgeMetrics metrics = new GateForgeMetrics(registry);

        metrics.recordMigration("demo-api", "applied");

        assertEquals(1.0, registry.find("gateforge_migrations_total")
                .tag("product", "demo-api")
                .tag("status", "applied")
                .counter()
                .count());
    }

    @Test
    void migrationTimer_recordsDuration() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        GateForgeMetrics metrics = new GateForgeMetrics(registry);

        var sample = metrics.startMigrationTimer();
        metrics.stopMigrationTimer(sample);

        assertNotNull(registry.find("gateforge_migration_duration_seconds").timer());
    }

    @Test
    void recordChatRequest_andProductsDiscovered() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        GateForgeMetrics metrics = new GateForgeMetrics(registry);

        metrics.recordChatRequest("wizard");
        metrics.setProductsDiscovered(7);

        assertEquals(1.0, registry.find("gateforge_ai_chat_requests")
                .tag("type", "wizard")
                .counter()
                .count());
        assertEquals(7.0, registry.find("gateforge_products_discovered").gauge().value());
    }
}
