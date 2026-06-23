package io.apishift.service.export;

import io.apishift.service.support.ExportZipTestSupport;
import io.apishift.service.support.ExportMinimalFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExportArchiveExtractorTest {

    private Path fixtureRoot() {
        return ExportMinimalFixture.root();
    }

    @Test
    void extractZip_validArchive_extractsManifest(@TempDir Path tempDir) throws Exception {
        Path zipFile = tempDir.resolve("export-minimal.zip");
        ExportZipTestSupport.zipDirectory(fixtureRoot(), zipFile);
        Path target = tempDir.resolve("extracted");

        ExportArchiveExtractor.extractZip(zipFile, target);

        assertTrue(Files.isRegularFile(target.resolve("manifest.json")));
        assertTrue(Files.isDirectory(target.resolve("products/seed_alpha")));
    }

    @Test
    void extractZip_zipSlipEntry_rejected(@TempDir Path tempDir) throws Exception {
        Path zipFile = tempDir.resolve("evil.zip");
        ExportZipTestSupport.zipWithEntry(zipFile, "../../escape.txt", "pwnd".getBytes());
        Path target = tempDir.resolve("extracted");

        ExportImportException ex = assertThrows(
                ExportImportException.class,
                () -> ExportArchiveExtractor.extractZip(zipFile, target));
        assertTrue(ex.getMessage().contains("escapes target directory"));
        assertFalse(Files.exists(target.resolve("escape.txt")));
    }
}
