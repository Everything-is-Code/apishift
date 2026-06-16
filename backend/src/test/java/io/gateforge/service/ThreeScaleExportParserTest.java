package io.gateforge.service;

import io.gateforge.model.ThreeScaleProduct;
import io.gateforge.service.export.ExportParseException;
import io.gateforge.service.export.ExportParseResult;
import io.gateforge.service.export.ThreeScaleExportParser;
import io.gateforge.service.support.ReflectionTestSupport;
import io.gateforge.service.support.ExportMinimalFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ThreeScaleExportParserTest {

    private final ThreeScaleExportParser parser = new ThreeScaleExportParser();

    private Path fixtureRoot() {
        return ExportMinimalFixture.root();
    }

    @Test
    void parse_exportMinimal_producesTwoProducts() {
        ExportParseResult result = parser.parse(fixtureRoot());

        assertEquals(2, result.products().size());
        assertEquals("1.0", result.manifest().schemaVersion());
    }

    @Test
    void parse_seedAlpha_mapsCoreFields() {
        ExportParseResult result = parser.parse(fixtureRoot());

        ThreeScaleProduct alpha = result.products().stream()
                .filter(p -> "seed_alpha".equals(p.systemName()))
                .findFirst()
                .orElseThrow();

        assertEquals("Seed Alpha Product", alpha.name());
        assertEquals(10L, alpha.serviceId());
        assertTrue(alpha.source().contains("export-v1"));
        assertEquals("offline", alpha.sourceCluster());
        assertEquals(ThreeScaleAuthMode.API_KEY, ThreeScaleAuthMode.fromProduct(alpha));
        assertEquals(1, alpha.applicationPlans().size());
        assertEquals("basic", alpha.applicationPlans().get(0).systemName());
        assertEquals(1, alpha.applications().size());
        assertEquals("Alpha App", alpha.applications().get(0).name());
    }

    @Test
    void parse_seedAlpha_resolvesBackendUsages() {
        ExportParseResult result = parser.parse(fixtureRoot());

        ThreeScaleProduct alpha = result.products().stream()
                .filter(p -> "seed_alpha".equals(p.systemName()))
                .findFirst()
                .orElseThrow();

        assertEquals(1, alpha.backendUsages().size());
        assertEquals("shared_payments", alpha.backendUsages().get(0).backendName());
        assertEquals("/payments", alpha.backendUsages().get(0).path());
    }

    @Test
    void parse_acceptsBackendUsagesArrayFormat(@TempDir Path tempDir) throws Exception {
        writeMinimalExportTree(tempDir);
        Files.writeString(tempDir.resolve("products/seed_alpha/backend_usages.json"), """
                [
                  {
                    "backend_usage": {
                      "backend_id": 1,
                      "path": "/payments"
                    }
                  }
                ]
                """);
        Files.writeString(tempDir.resolve("products/seed_list.yaml"), """
                apiVersion: v1
                kind: List
                items:
                - apiVersion: capabilities.3scale.net/v1beta1
                  kind: Product
                  spec:
                    name: Seed List Product
                    systemName: seed_list
                """);
        Files.createDirectories(tempDir.resolve("products/seed_list"));
        Files.writeString(tempDir.resolve("products/seed_list/proxy.json"), """
                {"proxy":{"service_id":11,"auth_type":"api_key","api_backend":{"path":"/"},"mapping_rules":[]}}
                """);

        ExportParseResult result = parser.parse(tempDir);
        ThreeScaleProduct alpha = result.products().stream()
                .filter(p -> "seed_alpha".equals(p.systemName()))
                .findFirst()
                .orElseThrow();
        ThreeScaleProduct listProduct = result.products().stream()
                .filter(p -> "seed_list".equals(p.systemName()))
                .findFirst()
                .orElseThrow();

        assertEquals(1, alpha.backendUsages().size());
        assertEquals("/payments", alpha.backendUsages().get(0).path());
        assertEquals("Seed List Product", listProduct.name());
    }

    @Test
    void parse_apiKeyProduct_ignoresIncidentalOidcConfiguration(@TempDir Path tempDir) throws Exception {
        writeMinimalExportTree(tempDir);
        Files.createDirectories(tempDir.resolve("products/seed_api_key"));
        Files.writeString(tempDir.resolve("products/seed_api_key/proxy.json"), """
                {"proxy":{"service_id":4,"auth_user_key":"true","api_backend":{"path":"/"},"mapping_rules":[]}}
                """);
        Files.writeString(tempDir.resolve("products/seed_api_key/oidc_configuration.json"), """
                {"oidc_configuration":{"standard_flow_enabled":true}}
                """);

        ExportParseResult result = parser.parse(tempDir);
        ThreeScaleProduct apiKey = result.products().stream()
                .filter(p -> "seed_api_key".equals(p.systemName()))
                .findFirst()
                .orElseThrow();

        assertEquals(ThreeScaleAuthMode.API_KEY, ThreeScaleAuthMode.fromProduct(apiKey));
    }

    private void writeMinimalExportTree(Path root) throws Exception {
        Files.createDirectories(root.resolve("products/seed_alpha"));
        Files.createDirectories(root.resolve("backends"));
        Files.writeString(root.resolve("manifest.json"), """
                {"schema_version":"1.0","admin_url":"https://example.com","product_count":1}
                """);
        Files.writeString(root.resolve("backends/shared_payments.json"), """
                {"id":1,"system_name":"shared_payments","name":"Payments","private_endpoint":"http://payments:8080"}
                """);
        Files.writeString(root.resolve("products/seed_alpha.yaml"), """
                name: Seed Alpha Product
                systemName: seed_alpha
                """);
        Files.writeString(root.resolve("products/seed_alpha/proxy.json"), """
                {"proxy":{"service_id":10,"auth_type":"api_key","api_backend":{"path":"/"},"mapping_rules":[]}}
                """);
        Files.writeString(root.resolve("products/seed_alpha/application_plans.json"), """
                {"plans":[{"application_plan":{"id":1,"name":"Basic","system_name":"basic","state":"published"}}]}
                """);
    }

    @Test
    void parse_rejectsUnsupportedSchema(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("manifest.json"), """
                {"schema_version":"2.0","admin_url":"https://example.com","product_count":0}
                """);

        ExportParseException ex = assertThrows(ExportParseException.class, () -> parser.parse(tempDir));
        assertTrue(ex.getMessage().contains("schema_version"));
    }

    @Test
    void loadFromExport_overridesListProducts() {
        ThreeScaleService service = new ThreeScaleService();
        ReflectionTestSupport.inject(service, "exportParser", parser);

        service.loadFromExport(fixtureRoot());

        assertEquals(2, service.listProducts().size());
        assertTrue(service.listProducts().stream()
                .allMatch(p -> p.source().contains("export-v1")));

        service.clearExportOverride();
    }

    @Test
    void refreshProductForMigration_exportSource_unchanged() {
        ThreeScaleProduct exported = parser.parse(fixtureRoot()).products().stream()
                .filter(p -> "seed_alpha".equals(p.systemName()))
                .findFirst()
                .orElseThrow();

        ThreeScaleService service = new ThreeScaleService();
        ThreeScaleProduct refreshed = service.refreshProductForMigration(exported);

        assertEquals(exported.authentication(), refreshed.authentication());
        assertEquals(exported.applicationPlans(), refreshed.applicationPlans());
        assertEquals(exported.applications(), refreshed.applications());
    }
}
