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
@TestProfile(AuthReadProtectionTestProfile.class)
class AuthReadProtectionTest {

    @InjectMock
    ThreeScaleService threeScaleService;

    @BeforeEach
    void stubThreeScale() {
        when(threeScaleService.listProducts()).thenReturn(List.of());
        when(threeScaleService.getAdminApiStatus()).thenReturn(Map.of("configured", true, "reachable", true));
    }

    @Test
    void getPlan_withoutAuth_returns401() {
        given()
                .get("/api/migration/plans/missing-plan")
                .then()
                .statusCode(401);
    }

    @Test
    void listPlans_asViewer_allowed() {
        given()
                .auth().preemptive().basic("viewer", "viewer")
                .get("/api/migration/plans")
                .then()
                .statusCode(200);
    }

    @Test
    void apply_asViewer_returns403() {
        given()
                .auth().preemptive().basic("viewer", "viewer")
                .contentType("application/json")
                .body("{}")
                .post("/api/migration/plans/missing-plan/apply")
                .then()
                .statusCode(403);
    }

    @Test
    void policyMapping_withoutAuth_allowed() {
        given()
                .get("/api/migration/policy-mapping")
                .then()
                .statusCode(200);
    }

    @Test
    void clusterFeatures_withoutAuth_allowed() {
        given()
                .get("/api/cluster/features")
                .then()
                .statusCode(200);
    }

    @Test
    void threescaleStatus_withoutAuth_allowed() {
        given()
                .get("/api/threescale/status")
                .then()
                .statusCode(200);
    }

    @Test
    void chatStatus_withoutAuth_allowed() {
        given()
                .get("/api/chat/status")
                .then()
                .statusCode(200);
    }
}
