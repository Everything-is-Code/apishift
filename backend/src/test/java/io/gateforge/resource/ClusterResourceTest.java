package io.gateforge.resource;

import io.gateforge.model.ClusterReadiness;
import io.gateforge.model.ProjectInfo;
import io.gateforge.service.ClusterReadinessService;
import io.gateforge.service.ClusterService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class ClusterResourceTest {

    @InjectMock
    ClusterService clusterService;

    @InjectMock
    ClusterReadinessService clusterReadinessService;

    @BeforeEach
    void stubClusterServices() {
        when(clusterService.listProjects()).thenReturn(List.of(
                new ProjectInfo("demo-ns", "Active", "2026-01-01T00:00:00Z", true, false)));
        when(clusterService.getProject("demo-ns")).thenReturn(
                new ProjectInfo("demo-ns", "Active", "2026-01-01T00:00:00Z", true, false));
        when(clusterService.getProject("missing")).thenReturn(null);
        when(clusterReadinessService.probe(anyString(), anyString()))
                .thenReturn(new ClusterReadiness(true, "local", "satisfied", List.of()));
    }

    @Test
    void listProjects_returnsProjectSummaries() {
        given()
                .when()
                .get("/api/cluster/projects")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].name", equalTo("demo-ns"));
    }

    @Test
    void getProject_notFound_returns404() {
        given()
                .when()
                .get("/api/cluster/projects/missing")
                .then()
                .statusCode(404);
    }

    @Test
    void getFeatures_returnsDeveloperHubFeature() {
        given()
                .when()
                .get("/api/cluster/features")
                .then()
                .statusCode(200)
                .body("developerHub.enabled", equalTo(false));
    }

    @Test
    void removeLocalCluster_returns400() {
        given()
                .when()
                .delete("/api/cluster/targets/local")
                .then()
                .statusCode(400);
    }

    @Test
    void validateTargetCluster_returnsValidationMap() {
        given()
                .when()
                .get("/api/cluster/targets/local/validate")
                .then()
                .statusCode(200)
                .body("clusterId", equalTo("local"));
    }

    @Test
    void getReadiness_returnsProbeResult() {
        given()
                .queryParam("targetClusterId", "local")
                .queryParam("planId", "plan-1")
                .when()
                .get("/api/cluster/readiness")
                .then()
                .statusCode(200)
                .body("clusterConnected", equalTo(true));
    }
}
