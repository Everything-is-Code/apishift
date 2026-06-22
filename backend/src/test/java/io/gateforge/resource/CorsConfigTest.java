package io.gateforge.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class CorsConfigTest {

    @Test
    void cors_allowedOrigin_returnsAccessControlAllowOrigin() {
        given()
                .header("Origin", "http://localhost:4200")
                .when()
                .get("/api/cluster/features")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:4200"));
    }

    @Test
    void cors_blockedOrigin_rejectsRequest() {
        given()
                .header("Origin", "https://evil.example.com")
                .when()
                .get("/api/cluster/features")
                .then()
                .statusCode(403);
    }
}
