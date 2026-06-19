package io.gateforge.resource;

import io.gateforge.model.APICastConfig;
import io.gateforge.model.MigrationPlan;
import io.gateforge.service.APICastDiscoveryService;
import io.gateforge.service.APICastToIstioMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
class APICastResourceTest {

    @InjectMock
    APICastDiscoveryService discoveryService;

    @InjectMock
    APICastToIstioMapper mapper;

    private static APICastConfig sampleConfig() {
        return new APICastConfig(
                "demo-apim",
                "apicast-ns",
                "ready",
                new APICastConfig.APICastDeploymentSpec(1, "100m", "128Mi"),
                new APICastConfig.APICastDeploymentSpec(2, "200m", "256Mi"),
                List.of(),
                new APICastConfig.TLSConfig(true, 0, null),
                null,
                new APICastConfig.OpenTracingConfig(true, "jaeger", null),
                List.of(),
                Map.of(),
                3L,
                2L);
    }

    @BeforeEach
    void stubDiscovery() {
        when(discoveryService.discoverAllAPIManagers()).thenReturn(List.of(sampleConfig()));
        when(discoveryService.discoverByNamespace("apicast-ns")).thenReturn(List.of(sampleConfig()));
        when(discoveryService.discoverByName("demo-apim", "apicast-ns")).thenReturn(sampleConfig());
        when(mapper.mapAPICastToIstio(any())).thenReturn(List.of(
                new MigrationPlan.GeneratedResource("Gateway", "gw", "apicast-ns", "kind: Gateway")));
        when(mapper.mapMultipleAPICasts(any())).thenReturn(List.of(
                List.of(new MigrationPlan.GeneratedResource("Gateway", "gw", "apicast-ns", "kind: Gateway"))));
    }

    @Test
    void discoverAll_returnsApiManagers() {
        given()
                .when()
                .get("/api/apicast/discover")
                .then()
                .statusCode(200)
                .body("total", equalTo(1))
                .body("apiManagers[0].name", equalTo("demo-apim"));
    }

    @Test
    void discoverByNamespace_scopesResults() {
        given()
                .when()
                .get("/api/apicast/discover/apicast-ns")
                .then()
                .statusCode(200)
                .body("namespace", equalTo("apicast-ns"))
                .body("total", equalTo(1));
    }

    @Test
    void analyze_found_returnsConfig() {
        given()
                .when()
                .get("/api/apicast/analyze/apicast-ns/demo-apim")
                .then()
                .statusCode(200)
                .body("apiManagerName", equalTo("demo-apim"));
    }

    @Test
    void analyze_notFound_returns404() {
        when(discoveryService.discoverByName(eq("missing"), eq("apicast-ns"))).thenReturn(null);

        given()
                .when()
                .get("/api/apicast/analyze/apicast-ns/missing")
                .then()
                .statusCode(404)
                .body("error", equalTo("APIManager not found or not self-managed"));
    }

    @Test
    void map_missingFields_returns400() {
        given()
                .contentType("application/json")
                .body(Map.of("namespace", "apicast-ns"))
                .when()
                .post("/api/apicast/map")
                .then()
                .statusCode(400)
                .body("error", equalTo("namespace and name are required"));
    }

    @Test
    void map_knownManager_returnsResources() {
        given()
                .contentType("application/json")
                .body(Map.of("namespace", "apicast-ns", "name", "demo-apim"))
                .when()
                .post("/api/apicast/map")
                .then()
                .statusCode(200)
                .body("apiManager", equalTo("demo-apim"))
                .body("total", equalTo(1));
    }

    @Test
    void mapAll_returnsPlans() {
        given()
                .when()
                .post("/api/apicast/map-all")
                .then()
                .statusCode(200)
                .body("apiManagers", equalTo(1))
                .body("totalResources", equalTo(1));
    }
}
