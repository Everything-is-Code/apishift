package io.gateforge.service;

import io.gateforge.model.AuditEntry;
import io.gateforge.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    @Test
    void record_persistsEntryThroughMigrationService() {
        List<AuditEntry> log = new ArrayList<>();
        MigrationService migrationService = mock(MigrationService.class);
        when(migrationService.getAuditLog()).thenReturn(log);

        AuditService auditService = new AuditService();
        ReflectionTestSupport.inject(auditService, "migrationService", migrationService);

        AuditEntry entry = auditService.record(
                "apply", "Gateway", "shared-gw", "kuadrant-system", null, "kind: Gateway");

        verify(migrationService).addAuditEntry(entry);
        assertEquals("apply", entry.action());
        assertEquals("gateforge-agent", entry.performedBy());
    }

    @Test
    void getAll_returnsMigrationServiceAuditLog() {
        List<AuditEntry> log = List.of();
        MigrationService migrationService = mock(MigrationService.class);
        when(migrationService.getAuditLog()).thenReturn(log);

        AuditService auditService = new AuditService();
        ReflectionTestSupport.inject(auditService, "migrationService", migrationService);

        assertSame(log, auditService.getAll());
    }
}
