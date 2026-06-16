package io.gateforge;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class OpenApiBuildTest {

  @Test
  void openapiDocumentIsAvailable() {
    given()
        .accept(ContentType.JSON)
        .when()
        .get("/q/openapi")
        .then()
        .statusCode(200)
        .body("openapi", notNullValue())
        .body("info.title", equalTo("GateForge API"));
  }
}
