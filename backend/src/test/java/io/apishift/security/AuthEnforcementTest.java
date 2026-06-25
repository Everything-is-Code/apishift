package io.apishift.security;

import io.apishift.service.ThreeScaleService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(AuthEnabledTestProfile.class)
class AuthEnforcementTest {

    @InjectMock
    ThreeScaleService threeScaleService;

    @BeforeEach
    void stubThreeScale() {
        when(threeScaleService.listProducts()).thenReturn(List.of());
        when(threeScaleService.refreshDiscovery()).thenReturn(Map.of("productCount", 0));
    }

    @Test
    void apply_withoutAuth_returns401() {
        given()
                .contentType("application/json")
                .body("{}")
                .post("/api/migration/plans/missing-plan/apply")
                .then()
                .statusCode(401);
    }

    @Test
    void apply_asOperator_returns403() {
        given()
                .auth().preemptive().basic("operator", "operator")
                .contentType("application/json")
                .body("{}")
                .post("/api/migration/plans/missing-plan/apply")
                .then()
                .statusCode(403);
    }

    @Test
    void apply_asAdmin_notUnauthorized() {
        given()
                .auth().preemptive().basic("admin", "admin")
                .contentType("application/json")
                .body("{}")
                .post("/api/migration/plans/missing-plan/apply")
                .then()
                .statusCode(not(anyOf(is(401), is(403))));
    }

    @Test
    void refresh_asOperator_allowed() {
        given()
                .auth().preemptive().basic("operator", "operator")
                .post("/api/threescale/refresh")
                .then()
                .statusCode(not(anyOf(is(401), is(403))));
    }

    @Test
    void listPlans_withoutAuth_allowedInPhase1() {
        given()
                .get("/api/migration/plans")
                .then()
                .statusCode(200);
    }

    @Test
    void healthReady_withoutAuth_allowed() {
        given()
                .get("/q/health/ready")
                .then()
                .statusCode(anyOf(is(200), is(503)));
    }

    @Test
    void mcp_withoutAuth_denied() {
        given()
                .get("/mcp")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }
}
