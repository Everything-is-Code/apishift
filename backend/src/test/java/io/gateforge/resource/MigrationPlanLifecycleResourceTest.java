package io.gateforge.resource;

import io.gateforge.ai.MigrationAgent;
import io.gateforge.service.ClusterReadinessService;
import io.gateforge.service.ClusterRegistry;
import io.gateforge.service.KuadrantCtlService;
import io.gateforge.service.ThreeScaleService;
import io.gateforge.service.export.ThreeScaleExportParser;
import io.gateforge.service.support.ExportMinimalFixture;
import io.gateforge.service.support.MigrationKubernetesTestSupport;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class MigrationPlanLifecycleResourceTest {

    @InjectMock
    ThreeScaleService threeScaleService;

    @InjectMock
    MigrationAgent migrationAgent;

    @InjectMock
    KuadrantCtlService kuadrantCtlService;

    @InjectMock
    ClusterReadinessService clusterReadinessService;

    @InjectMock
    ClusterRegistry clusterRegistry;

    @BeforeEach
    void stubDependencies() {
        var products = new ThreeScaleExportParser().parse(ExportMinimalFixture.root()).products();
        when(threeScaleService.listProducts()).thenReturn(products);
        when(threeScaleService.refreshProductForMigration(any())).thenAnswer(inv -> inv.getArgument(0));
        when(migrationAgent.chat(anyString())).thenReturn("Smoke verification OK");
        when(kuadrantCtlService.generateHttpRoute(anyString())).thenReturn("ERROR");
        when(clusterReadinessService.enrich(any(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        var kubernetesClient = MigrationKubernetesTestSupport.inSyncClient();
        when(clusterRegistry.getClient(anyString())).thenReturn(kubernetesClient);
        when(clusterRegistry.getCluster(anyString())).thenReturn(io.gateforge.model.TargetCluster.local());
    }

    @Test
    void applyPlan_afterAnalyze_appliesResources() {
        String planId = analyzeSharedPlan();

        given()
                .contentType("application/json")
                .body(Map.of())
                .when()
                .post("/api/migration/plans/" + planId + "/apply")
                .then()
                .statusCode(200)
                .body("planId", equalTo(planId))
                .body("applied", greaterThan(0))
                .body("failed", equalTo(0));
    }

    @Test
    void checkDrift_inSyncCluster_reportsInSync() {
        String planId = analyzeSharedPlan();

        given()
                .when()
                .get("/api/migration/plans/" + planId + "/drift")
                .then()
                .statusCode(200)
                .body("status", everyItem(equalTo("in-sync")));
    }

    @Test
    void checkDrift_missingResources_reportsMissing() {
        var missingClient = MigrationKubernetesTestSupport.missingClient();
        when(clusterRegistry.getClient(anyString())).thenReturn(missingClient);
        String planId = analyzeSharedPlan();

        given()
                .when()
                .get("/api/migration/plans/" + planId + "/drift")
                .then()
                .statusCode(200)
                .body("status", everyItem(equalTo("missing")));
    }

    @Test
    void revertPlan_afterApply_marksPlanReverted() {
        String planId = analyzeSharedPlan();

        given()
                .contentType("application/json")
                .body(Map.of())
                .post("/api/migration/plans/" + planId + "/apply");

        given()
                .contentType("application/json")
                .when()
                .post("/api/migration/plans/" + planId + "/revert")
                .then()
                .statusCode(200)
                .body("failed", equalTo(0));

        given()
                .when()
                .get("/api/migration/plans/" + planId)
                .then()
                .statusCode(200)
                .body("status", equalTo("REVERTED"));
    }

    @Test
    void listPlans_afterAnalyze_includesCreatedPlan() {
        String planId = analyzeSharedPlan();

        given()
                .when()
                .get("/api/migration/plans")
                .then()
                .statusCode(200)
                .body("id", hasItem(planId));
    }

    @Test
    void getTestCommands_afterAnalyze_returnsCurlCommands() {
        String planId = analyzeSharedPlan();

        given()
                .when()
                .get("/api/migration/plans/" + planId + "/test-commands")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].command", containsString("curl"));
    }

    @Test
    void getCatalogInfo_buildsComponentYaml() {
        String planId = analyzeSharedPlan();

        given()
                .when()
                .get("/api/migration/plans/" + planId + "/catalog-info/Seed Alpha Product")
                .then()
                .statusCode(200)
                .contentType(containsString("yaml"))
                .body(containsString("kind: Component"));
    }

    @Test
    void policyMapping_returnsReference() {
        given()
                .when()
                .get("/api/migration/policy-mapping")
                .then()
                .statusCode(200)
                .body("consolidated.size()", greaterThan(0));
    }

    @Test
    void applyPlan_withExcludedIndex_skipsResource() {
        String planId = analyzeSharedPlan();

        given()
                .contentType("application/json")
                .body(Map.of("excludedIndexes", List.of(0)))
                .when()
                .post("/api/migration/plans/" + planId + "/apply")
                .then()
                .statusCode(200)
                .body("results[0].message", equalTo("Skipped"));
    }

    @Test
    void getPlan_unknownId_returns404() {
        given()
                .when()
                .get("/api/migration/plans/does-not-exist")
                .then()
                .statusCode(404);
    }

    @Test
    void revertBulk_afterAnalyze_revertsPlans() {
        String planId = analyzeSharedPlan();

        given()
                .contentType("application/json")
                .body(Map.of("planIds", List.of(planId), "deleteGateway", false))
                .when()
                .post("/api/migration/revert-bulk")
                .then()
                .statusCode(200)
                .body("totalPlans", equalTo(1));
    }

    private String analyzeSharedPlan() {
        return given()
                .contentType("application/json")
                .body(Map.of(
                        "gatewayStrategy", "shared",
                        "products", List.of("Seed Alpha Product"),
                        "targetClusterId", "local"))
                .when()
                .post("/api/migration/analyze")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
    }
}
