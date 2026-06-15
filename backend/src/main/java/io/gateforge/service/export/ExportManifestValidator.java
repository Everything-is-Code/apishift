package io.gateforge.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExportManifestValidator {

    private static final String EXPECTED_SCHEMA = "1.0";

    private ExportManifestValidator() {}

    public static ExportManifest validateAndRead(Path exportRoot, ObjectMapper objectMapper) {
        Path manifestPath = exportRoot.resolve("manifest.json");
        if (!Files.isRegularFile(manifestPath)) {
            throw new ExportParseException("manifest.json is missing in export directory");
        }
        try {
            ExportManifest manifest = objectMapper.readValue(Files.readString(manifestPath), ExportManifest.class);
            if (manifest.schemaVersion() == null || !EXPECTED_SCHEMA.equals(manifest.schemaVersion())) {
                throw new ExportParseException(
                        "Unsupported export schema_version: " + manifest.schemaVersion());
            }
            return manifest;
        } catch (ExportParseException e) {
            throw e;
        } catch (IOException e) {
            throw new ExportParseException("Failed to read manifest.json", e);
        }
    }
}
