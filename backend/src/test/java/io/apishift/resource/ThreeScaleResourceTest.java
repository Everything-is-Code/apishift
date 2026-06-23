package io.apishift.resource;

import io.apishift.model.ThreeScaleProduct;
import io.apishift.service.ThreeScaleService;
import io.apishift.service.support.MigrationFixtures;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
class ThreeScaleResourceTest {

    @InjectMock
    ThreeScaleService threeScaleService;

    @BeforeEach
    void stubThreeScale() {
        ThreeScaleProduct product = MigrationFixtures.apiKeyProduct();
        when(threeScaleService.listProducts()).thenReturn(List.of(product));
        when(threeScaleService.getProduct("nonexistent-product-xyz", "default")).thenReturn(null);
        when(threeScaleService.listBackendsCombined()).thenReturn(List.of(
                Map.of("name", "api-backend", "namespace", "default")));
        when(threeScaleService.getAdminApiStatus()).thenReturn(Map.of("configured", false, "crdDiscoveryEnabled", false));
        when(threeScaleService.refreshDiscovery()).thenReturn(Map.of(
                "productCount", 0, "backendCount", 0, "refreshedAt", "2026-06-19T00:00:00Z"));
    }

    @Test
    void listProducts_returnsJsonArray() {
        given()
                .when()
                .get("/api/threescale/products")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    @Test
    void getProduct_notFound_returns404() {
        given()
                .when()
                .get("/api/threescale/products/default/nonexistent-product-xyz")
                .then()
                .statusCode(404);
    }

    @Test
    void listBackends_returnsJsonArray() {
        given()
                .when()
                .get("/api/threescale/backends")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    @Test
    void getStatus_returnsAdminStatusShape() {
        given()
                .when()
                .get("/api/threescale/status")
                .then()
                .statusCode(200)
                .body("crdDiscoveryEnabled", equalTo(false));
    }

    @Test
    void refreshDiscovery_returnsCounts() {
        given()
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/api/threescale/refresh")
                .then()
                .statusCode(200)
                .body("productCount", equalTo(0))
                .body("backendCount", equalTo(0));
    }

    @Test
    void removeDefaultSource_returns400() {
        given()
                .when()
                .delete("/api/threescale/sources/default")
                .then()
                .statusCode(400);
    }

    @Test
    void getSourceStatus_unknownSource_returnsError() {
        given()
                .when()
                .get("/api/threescale/sources/unknown-source/status")
                .then()
                .statusCode(200)
                .body("error", equalTo("Source not found"));
    }
}
