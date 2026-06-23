package io.apishift.service.export;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ExportArchiveExtractor {

    private ExportArchiveExtractor() {}

    public static void extractZip(Path zipFile, Path targetDir) throws IOException {
        if (zipFile == null || !Files.isRegularFile(zipFile)) {
            throw new ExportImportException("Zip file is missing or not readable");
        }
        if (targetDir == null) {
            throw new ExportImportException("Target directory is required");
        }
        Files.createDirectories(targetDir);
        Path normalizedTarget = targetDir.toAbsolutePath().normalize();

        try (InputStream input = Files.newInputStream(zipFile);
             ZipInputStream zipInput = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                Path resolved = normalizedTarget.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(normalizedTarget)) {
                    throw new ExportImportException(
                            "Zip entry escapes target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    if (resolved.getParent() != null) {
                        Files.createDirectories(resolved.getParent());
                    }
                    Files.copy(zipInput, resolved, StandardCopyOption.REPLACE_EXISTING);
                }
                zipInput.closeEntry();
            }
        }
    }
}
