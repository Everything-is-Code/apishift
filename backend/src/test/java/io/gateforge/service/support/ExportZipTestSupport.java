package io.gateforge.service.support;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ExportZipTestSupport {

    private ExportZipTestSupport() {}

    public static Path zipDirectory(Path sourceDir, Path zipFile) throws IOException {
        Path normalizedSource = sourceDir.toAbsolutePath().normalize();
        try (OutputStream output = Files.newOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(output)) {
            try (Stream<Path> paths = Files.walk(normalizedSource)) {
                for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                    String entryName = normalizedSource.relativize(path).toString().replace('\\', '/');
                    zipOut.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zipOut);
                    zipOut.closeEntry();
                }
            }
        }
        return zipFile;
    }

    public static Path zipWithEntry(Path zipFile, String entryName, byte[] content) throws IOException {
        try (OutputStream output = Files.newOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(output)) {
            zipOut.putNextEntry(new ZipEntry(entryName));
            zipOut.write(content);
            zipOut.closeEntry();
        }
        return zipFile;
    }
}
