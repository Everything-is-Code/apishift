package io.gateforge.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CredentialRedactionResourceTest {

    @Test
    void listSources_jsonOmitsAccessToken() {
        given()
                .contentType("application/json")
                .body(Map.of(
                        "id", "redaction-test-source",
                        "label", "Redaction test",
                        "adminUrl", "https://3scale-redaction.example.com",
                        "accessToken", "super-secret-access-token",
                        "enabled", true))
                .when()
                .post("/api/threescale/sources")
                .then()
                .statusCode(200)
                .body("id", equalTo("redaction-test-source"))
                .body("credentialConfigured", equalTo(true));

        Response response = given()
                .when()
                .get("/api/threescale/sources");

        response.then()
                .statusCode(200)
                .body("id", hasItem("redaction-test-source"))
                .body("credentialConfigured", hasItem(true));

        assertFalse(response.asString().contains("accessToken"),
                "GET /api/threescale/sources must not expose accessToken");

        given()
                .when()
                .delete("/api/threescale/sources/redaction-test-source")
                .then()
                .statusCode(204);
    }

    @Test
    void listTargets_jsonOmitsToken() {
        given()
                .contentType("application/json")
                .body(Map.of(
                        "id", "redaction-test-cluster",
                        "label", "Redaction cluster",
                        "apiServerUrl", "https://api.redaction.example.com",
                        "token", "super-secret-kube-token",
                        "authType", "token",
                        "verifySsl", false,
                        "enabled", true))
                .when()
                .post("/api/cluster/targets")
                .then()
                .statusCode(200)
                .body("id", equalTo("redaction-test-cluster"))
                .body("credentialConfigured", equalTo(true));

        Response response = given()
                .when()
                .get("/api/cluster/targets");

        response.then()
                .statusCode(200)
                .body("id", hasItem("redaction-test-cluster"))
                .body("id", hasItem("local"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> clusters = response.jsonPath().getList("$");
        for (Map<String, Object> cluster : clusters) {
            assertFalse(cluster.containsKey("token"),
                    "cluster " + cluster.get("id") + " must not expose token field");
        }

        Map<String, Object> local = clusters.stream()
                .filter(c -> "local".equals(c.get("id")))
                .findFirst()
                .orElseThrow();
        assertFalse((Boolean) local.get("credentialConfigured"));

        given()
                .when()
                .delete("/api/cluster/targets/redaction-test-cluster")
                .then()
                .statusCode(204);
    }

    @Test
    void hubOverview_jsonOmitsSecrets() {
        @SuppressWarnings("unchecked")
        Map<String, Object> overview = given()
                .when()
                .get("/api/hub/overview")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getMap("$");

        for (String key : overview.keySet()) {
            assertFalse(key.equals("accessToken") || key.equals("token"));
        }
        assertFalse(overview.toString().contains("accessToken="));
    }

    @Test
    void hubTopology_jsonOmitsSecrets() {
        @SuppressWarnings("unchecked")
        Map<String, Object> topology = given()
                .when()
                .get("/api/hub/topology")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getMap("$");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> clusters = (List<Map<String, Object>>) topology.get("clusters");
        for (Map<String, Object> cluster : clusters) {
            assertFalse(cluster.containsKey("token"));
            assertFalse(cluster.containsKey("accessToken"));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sources = (List<Map<String, Object>>) topology.get("sources");
        for (Map<String, Object> source : sources) {
            assertFalse(source.containsKey("accessToken"));
            assertFalse(source.containsKey("token"));
        }
    }
}
