package io.gateforge.service;

import io.gateforge.model.ThreeScaleProduct;
import io.gateforge.service.export.ExportParseException;
import io.gateforge.service.export.ExportParseResult;
import io.gateforge.service.export.ThreeScaleExportParser;
import io.gateforge.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ThreeScaleExportParserTest {

    private final ThreeScaleExportParser parser = new ThreeScaleExportParser();

    private Path fixtureRoot() {
        return Path.of("src/test/resources/export-minimal").toAbsolutePath().normalize();
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
