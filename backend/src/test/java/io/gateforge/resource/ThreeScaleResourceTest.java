package io.gateforge.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class ThreeScaleResourceTest {

    @Test
    void listProducts_returnsJsonArray() {
        given()
                .when()
                .get("/api/threescale/products")
                .then()
                .statusCode(200);
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
                .statusCode(200);
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
                .body("productCount", greaterThanOrEqualTo(0))
                .body("backendCount", greaterThanOrEqualTo(0));
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
