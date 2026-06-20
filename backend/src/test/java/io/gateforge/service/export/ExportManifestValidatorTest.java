package io.gateforge.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.service.support.ExportMinimalFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ExportManifestValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validateAndRead_validFixture_returnsManifest() {
        ExportManifest manifest = ExportManifestValidator.validateAndRead(
                ExportMinimalFixture.root(), objectMapper);

        assertEquals("1.0", manifest.schemaVersion());
        assertTrue(manifest.productCount() > 0);
    }

    @Test
    void validateAndRead_missingManifest_throws() throws Exception {
        Path temp = Files.createTempDirectory("export-no-manifest");
        try {
            assertThrows(ExportParseException.class,
                    () -> ExportManifestValidator.validateAndRead(temp, objectMapper));
        } finally {
            deleteRecursively(temp);
        }
    }

    @Test
    void validateAndRead_unsupportedSchema_throws() throws Exception {
        Path temp = Files.createTempDirectory("export-bad-schema");
        try {
            Files.writeString(temp.resolve("manifest.json"), """
                    {"schema_version":"9.9","products":[]}
                    """);
            assertThrows(ExportParseException.class,
                    () -> ExportManifestValidator.validateAndRead(temp, objectMapper));
        } finally {
            deleteRecursively(temp);
        }
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        }
    }
}
