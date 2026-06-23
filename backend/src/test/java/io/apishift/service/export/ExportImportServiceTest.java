package io.apishift.service.export;

import io.apishift.service.ThreeScaleService;
import io.apishift.service.support.ExportZipTestSupport;
import io.apishift.service.support.ReflectionTestSupport;
import io.apishift.service.support.ExportMinimalFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExportImportServiceTest {

    private Path fixtureRoot() {
        return ExportMinimalFixture.root();
    }

    @Test
    void importFromZip_exportMinimal_loadsProducts(@TempDir Path tempDir) throws Exception {
        Path zipFile = tempDir.resolve("export-minimal.zip");
        ExportZipTestSupport.zipDirectory(fixtureRoot(), zipFile);

        ExportImportService service = createService();
        ImportExportResponse response = service.importFromZip(zipFile);

        assertEquals("export-v1", response.importMode());
        assertEquals(2, response.productCount());
        assertEquals("1.0", response.manifest().schemaVersion());
        assertTrue(response.products().stream().anyMatch(p -> "seed_alpha".equals(p.systemName())));

        assertEquals(2, serviceBackedThreeScale(service).listProducts().size());
        serviceBackedThreeScale(service).clearExportOverride();
    }

    @Test
    void importFromZip_invalidManifest_clearsOverride(@TempDir Path tempDir) throws Exception {
        Path zipFile = tempDir.resolve("bad-manifest.zip");
        Files.writeString(tempDir.resolve("manifest.json"), """
                {"schema_version":"2.0","admin_url":"https://example.com","product_count":0}
                """);
        ExportZipTestSupport.zipDirectory(tempDir, zipFile);

        ExportImportService service = createService();
        assertThrows(ExportImportException.class, () -> service.importFromZip(zipFile));
        assertNull(overrideOf(serviceBackedThreeScale(service)));
    }

    @Test
    void importUpload_missingFile_rejected() {
        ExportImportService service = createService();
        ExportImportException ex = assertThrows(
                ExportImportException.class,
                () -> service.importUpload(null));
        assertTrue(ex.getMessage().contains("file"));
    }

    private ExportImportService createService() {
        ThreeScaleService threeScaleService = new ThreeScaleService();
        ReflectionTestSupport.inject(threeScaleService, "exportParser", new ThreeScaleExportParser());

        ExportImportService service = new ExportImportService();
        ReflectionTestSupport.inject(service, "threeScaleService", threeScaleService);
        ReflectionTestSupport.inject(service, "maxUploadMb", 50L);
        return service;
    }

    private ThreeScaleService serviceBackedThreeScale(ExportImportService service) {
        return (ThreeScaleService) ReflectionTestSupport.getField(service, "threeScaleService");
    }

    @SuppressWarnings("unchecked")
    private Object overrideOf(ThreeScaleService threeScaleService) {
        return ReflectionTestSupport.getField(threeScaleService, "exportOverride");
    }
}
