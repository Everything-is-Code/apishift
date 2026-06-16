package io.gateforge.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Feature flags exposed to the UI")
public record ClusterFeatures(
        DeveloperHubFeature developerHub) {

    @Schema(description = "Red Hat Developer Hub integration")
    public record DeveloperHubFeature(
            boolean enabled,
            String url) {
    }
}
