package io.gateforge.service;

import io.gateforge.model.MigrationPlan;
import io.gateforge.service.export.ThreeScaleExportParser;
import io.gateforge.service.support.MigrationServiceTestSupport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThreeScaleExportAnalyzeTest extends MigrationServiceTestSupport {

    private final ThreeScaleExportParser parser = new ThreeScaleExportParser();

    @Test
    void analyze_exportSeedAlpha_generatesCoreKinds() {
        var products = parser.parse(fixtureRoot()).products();
        MigrationServiceForTest service = MigrationServiceForTest.createWithProducts(products);

        MigrationPlan plan = service.analyze("shared", List.of("Seed Alpha Product"), "local");

        assertNotNull(plan);
        assertTrue(hasKind(plan, "Gateway"));
        assertTrue(hasKind(plan, "HTTPRoute"));
        assertTrue(hasKind(plan, "AuthPolicy"));
        assertTrue(hasKind(plan, "RateLimitPolicy"));
        assertEquals("gateforge-shared", resourceNames(plan, "Gateway").get(0));
    }

    private static Path fixtureRoot() {
        return Path.of("src/test/resources/export-minimal").toAbsolutePath().normalize();
    }
}
