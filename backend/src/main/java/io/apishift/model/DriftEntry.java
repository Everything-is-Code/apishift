package io.apishift.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Drift status for a generated resource on the target cluster")
public record DriftEntry(
        String kind,
        String name,
        String namespace,
        @Schema(description = "in-sync, missing, or error") String status,
        String message) {
}
