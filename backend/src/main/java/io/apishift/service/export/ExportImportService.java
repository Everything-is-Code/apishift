package io.apishift.service.export;

import io.apishift.model.ThreeScaleProduct;
import io.apishift.service.ThreeScaleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@ApplicationScoped
public class ExportImportService {

    private static final String IMPORT_MODE = "export-v1";

    @Inject
    ThreeScaleService threeScaleService;

    @ConfigProperty(name = "ApiShift.export.max-upload-mb", defaultValue = "50")
    long maxUploadMb;

    public ImportExportResponse importUpload(FileUpload upload) {
        if (upload == null || upload.uploadedFile() == null) {
            throw new ExportImportException("Multipart field 'file' is required");
        }
        String fileName = upload.fileName() != null ? upload.fileName().toLowerCase(Locale.ROOT) : "";
        if (!fileName.endsWith(".zip")) {
            throw new ExportImportException("Only .zip export archives are supported");
        }
        long maxBytes = maxUploadMb * 1024L * 1024L;
        if (upload.size() > maxBytes) {
            throw new ExportImportException(
                    "Export archive exceeds maximum size of " + maxUploadMb + " MB");
        }
        return importFromZip(upload.uploadedFile());
    }

    public ImportExportResponse importFromZip(Path zipFile) {
        Path extractDir = null;
        try {
            extractDir = Files.createTempDirectory("apishift-export-");
            ExportArchiveExtractor.extractZip(zipFile, extractDir);
            ExportParseResult result = threeScaleService.loadFromExport(extractDir);
            return toResponse(result);
        } catch (ExportParseException e) {
            threeScaleService.clearExportOverride();
            throw new ExportImportException(e.getMessage(), e);
        } catch (ExportImportException e) {
            threeScaleService.clearExportOverride();
            throw e;
        } catch (IOException e) {
            threeScaleService.clearExportOverride();
            throw new ExportImportException("Failed to import export archive", e);
        } finally {
            if (extractDir != null) {
                deleteRecursively(extractDir);
            }
        }
    }

    private ImportExportResponse toResponse(ExportParseResult result) {
        List<ImportExportResponse.ProductSummary> products = result.products().stream()
                .sorted(Comparator.comparing(ThreeScaleProduct::systemName))
                .map(p -> new ImportExportResponse.ProductSummary(
                        p.name(), p.systemName(), p.serviceId()))
                .toList();
        ExportManifest manifest = result.manifest();
        ImportExportResponse.ManifestSummary manifestSummary =
                new ImportExportResponse.ManifestSummary(
                        manifest.schemaVersion(),
                        manifest.adminUrl(),
                        manifest.exportedAt());
        return new ImportExportResponse(
                IMPORT_MODE,
                products.size(),
                products,
                manifestSummary);
    }

    private static void deleteRecursively(Path root) {
        try {
            if (!Files.exists(root)) {
                return;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // best-effort temp cleanup
                    }
                });
            }
        } catch (IOException ignored) {
            // best-effort temp cleanup
        }
    }
}
