package io.apishift.repository;

import io.apishift.model.MigrationPlan;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class MigrationPlanRepositoryTest {

    @Inject
    MigrationPlanRepository repository;

    @Test
    @Transactional
    void saveAndLoad_roundTripsConsolidationWarnings() {
        String planId = "test-" + UUID.randomUUID();
        List<String> warnings = List.of(
                "Product 'demo-api': APIProduct skipped — enable ApiShift.developer-hub.enabled for Developer Portal catalog tier."
        );
        MigrationPlan plan = new MigrationPlan(
                planId,
                "shared",
                List.of("demo-api"),
                List.of(new MigrationPlan.GeneratedResource("Gateway", "apishift-shared", "kuadrant-system", "kind: Gateway")),
                "analysis",
                Instant.now(),
                null,
                "ACTIVE",
                "local",
                "Local (in-cluster)",
                warnings,
                List.of()
        );

        repository.save(plan);

        MigrationPlan loaded = repository.findPlanById(planId);
        assertNotNull(loaded);
        assertEquals(warnings, loaded.consolidationWarnings());

        MigrationPlan summary = repository.listPlanSummaries().stream()
                .filter(p -> planId.equals(p.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(warnings, summary.consolidationWarnings());
    }
}
