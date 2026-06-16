package io.gateforge.resource;

import io.gateforge.ai.MigrationAgent;
import io.gateforge.service.ClusterReadinessService;
import io.gateforge.service.KuadrantCtlService;
import io.gateforge.service.ThreeScaleService;
import io.gateforge.service.export.ThreeScaleExportParser;
import io.gateforge.service.support.ExportMinimalFixture;
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
class MigrationAnalyzeResourceTest {

  @InjectMock
  ThreeScaleService threeScaleService;

  @InjectMock
  MigrationAgent migrationAgent;

  @InjectMock
  KuadrantCtlService kuadrantCtlService;

  @InjectMock
  ClusterReadinessService clusterReadinessService;

  @BeforeEach
  void stubDependencies() {
    var products = new ThreeScaleExportParser().parse(ExportMinimalFixture.root()).products();
    when(threeScaleService.listProducts()).thenReturn(products);
    when(threeScaleService.refreshProductForMigration(any())).thenAnswer(inv -> inv.getArgument(0));
    when(migrationAgent.chat(anyString())).thenReturn("Smoke verification OK");
    when(kuadrantCtlService.generateHttpRoute(anyString())).thenReturn("ERROR");
    when(clusterReadinessService.enrich(any(), anyString())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void analyze_fixtureProduct_returnsCoreResourceKinds() {
    given()
        .contentType("application/json")
        .body(Map.of(
            "gatewayStrategy", "shared",
            "products", List.of("Seed Alpha Product"),
            "targetClusterId", "local"))
        .when()
        .post("/api/migration/analyze")
        .then()
        .statusCode(200)
        .body("id", not(emptyOrNullString()))
        .body("gatewayStrategy", equalTo("shared"))
        .body("resources.kind", hasItems("Gateway", "HTTPRoute", "AuthPolicy", "RateLimitPolicy"));
  }
}
