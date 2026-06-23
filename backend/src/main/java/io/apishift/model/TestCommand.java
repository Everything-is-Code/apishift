package io.apishift.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Suggested curl command to validate a migrated route")
public record TestCommand(
        String label,
        String command,
        @Schema(description = "no-auth, bearer, api-key, or path-test") String type) {
}
