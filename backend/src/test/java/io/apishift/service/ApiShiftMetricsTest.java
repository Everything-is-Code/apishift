package io.apishift.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiShiftMetricsTest {

    @Test
    void recordMigration_incrementsCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiShiftMetrics metrics = new ApiShiftMetrics(registry);

        metrics.recordMigration("demo-api", "applied");

        assertEquals(1.0, registry.find("apishift_migrations_total")
                .tag("product", "demo-api")
                .tag("status", "applied")
                .counter()
                .count());
    }

    @Test
    void migrationTimer_recordsDuration() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiShiftMetrics metrics = new ApiShiftMetrics(registry);

        var sample = metrics.startMigrationTimer();
        metrics.stopMigrationTimer(sample);

        assertNotNull(registry.find("apishift_migration_duration_seconds").timer());
    }

    @Test
    void recordChatRequest_andProductsDiscovered() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiShiftMetrics metrics = new ApiShiftMetrics(registry);

        metrics.recordChatRequest("wizard");
        metrics.setProductsDiscovered(7);

        assertEquals(1.0, registry.find("apishift_ai_chat_requests")
                .tag("type", "wizard")
                .counter()
                .count());
        assertEquals(7.0, registry.find("apishift_products_discovered").gauge().value());
    }
}
