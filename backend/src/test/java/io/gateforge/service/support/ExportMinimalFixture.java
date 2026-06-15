package io.gateforge.service.support;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Extracts the versioned {@code export-minimal-1.0.tar.gz} fixture published by
 * <a href="https://github.com/Everything-is-Code/3scaleextract/tree/main/testdata">3scaleextract</a>
 * for offline import tests.
 */
public final class ExportMinimalFixture {

    private static final String ARTIFACT = "export-minimal-1.0.tar.gz";
    private static final Path FIXTURES_DIR =
            Path.of("src/test/resources/fixtures").toAbsolutePath().normalize();
    private static final Path TARBALL = FIXTURES_DIR.resolve(ARTIFACT);
    private static final Path CHECKSUM = FIXTURES_DIR.resolve(ARTIFACT + ".sha256");
    private static final Path EXTRACT_ROOT =
            Path.of("target/test-fixtures/export-minimal").toAbsolutePath().normalize();

    private static volatile Path cachedExportRoot;

    private ExportMinimalFixture() {}

    public static Path root() {
        Path cached = cachedExportRoot;
        if (cached != null) {
            return cached;
        }
        synchronized (ExportMinimalFixture.class) {
            if (cachedExportRoot != null) {
                return cachedExportRoot;
            }
            cachedExportRoot = ensureExtracted();
            return cachedExportRoot;
        }
    }

    private static Path ensureExtracted() {
        Path manifest = EXTRACT_ROOT.resolve("manifest.json");
        if (Files.isRegularFile(manifest)) {
            return EXTRACT_ROOT;
        }
        verifyChecksum();
        try {
            Files.createDirectories(EXTRACT_ROOT.getParent());
            if (Files.exists(EXTRACT_ROOT)) {
                deleteRecursively(EXTRACT_ROOT);
            }
            extractTarGz(TARBALL, EXTRACT_ROOT.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract export-minimal fixture from " + TARBALL, e);
        }
        if (!Files.isRegularFile(manifest)) {
            throw new IllegalStateException("Fixture extract missing manifest.json at " + manifest);
        }
        return EXTRACT_ROOT;
    }

    private static void verifyChecksum() {
        if (!Files.isRegularFile(TARBALL)) {
            throw new IllegalStateException("Missing fixture tarball: " + TARBALL);
        }
        if (!Files.isRegularFile(CHECKSUM)) {
            throw new IllegalStateException("Missing fixture checksum: " + CHECKSUM);
        }
        try {
            String checksumLine = Files.readString(CHECKSUM).trim();
            String expected = checksumLine.split("\\s+")[0].toLowerCase(Locale.ROOT);
            String actual = sha256Hex(TARBALL);
            if (!expected.equals(actual)) {
                throw new IllegalStateException(
                        "Fixture tarball checksum mismatch for " + TARBALL
                                + " (expected " + expected + ", got " + actual + ")");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to verify fixture checksum", e);
        }
    }

    private static String sha256Hex(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static void extractTarGz(Path tarball, Path destParent) throws IOException {
        Path normalizedParent = destParent.toAbsolutePath().normalize();
        try (InputStream fileIn = Files.newInputStream(tarball);
             GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(fileIn);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                Path resolved = normalizedParent.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(normalizedParent)) {
                    throw new IOException("Tar entry escapes destination: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    if (resolved.getParent() != null) {
                        Files.createDirectories(resolved.getParent());
                    }
                    Files.copy(tarIn, resolved);
                }
            }
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best-effort cleanup before re-extract
                }
            });
        }
    }
}
