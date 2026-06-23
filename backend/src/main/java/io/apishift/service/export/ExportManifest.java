package io.apishift.service.export;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExportManifest(
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("exported_at") String exportedAt,
        @JsonProperty("admin_url") String adminUrl,
        @JsonProperty("product_count") int productCount,
        @JsonProperty("backend_count") int backendCount,
        @JsonProperty("application_count") int applicationCount,
        @JsonProperty("include_applications") boolean includeApplications,
        @JsonProperty("incomplete") boolean incomplete) {}
