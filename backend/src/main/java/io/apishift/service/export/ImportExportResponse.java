package io.apishift.service.export;

import java.util.List;

public record ImportExportResponse(
        String importMode,
        int productCount,
        List<ProductSummary> products,
        ManifestSummary manifest) {

    public record ProductSummary(String name, String systemName, long serviceId) {}

    public record ManifestSummary(
            String schemaVersion,
            String adminUrl,
            String exportedAt) {}
}
