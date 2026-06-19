package io.gateforge.resource;

import io.gateforge.model.AuditEntry;
import io.gateforge.service.AuditService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
class AuditResourceTest {

    @InjectMock
    AuditService auditService;

    @BeforeEach
    void stubAuditLog() {
        when(auditService.getAll()).thenReturn(List.of(
                new AuditEntry("abc12345", Instant.parse("2026-06-19T12:00:00Z"),
                        "apply", "Gateway", "shared-gw", "kuadrant-system",
                        null, "kind: Gateway", "gateforge-agent")));
    }

    @Test
    void listReports_returnsAuditEntries() {
        given()
                .when()
                .get("/api/audit/reports")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].action", equalTo("apply"))
                .body("[0].resourceKind", equalTo("Gateway"));
    }
}
