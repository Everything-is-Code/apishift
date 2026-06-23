package io.apishift.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Target Kubernetes cluster (public view — credentials never exposed)")
public record TargetClusterView(
        String id,
        String label,
        String apiServerUrl,
        String authType,
        boolean verifySsl,
        boolean enabled,
        boolean credentialConfigured) {

    public static TargetClusterView from(TargetCluster cluster) {
        return new TargetClusterView(
                cluster.id(),
                cluster.label(),
                cluster.apiServerUrl(),
                cluster.authType(),
                cluster.verifySsl(),
                cluster.enabled(),
                hasCredential(cluster.token()));
    }

    static boolean hasCredential(String token) {
        return token != null && !token.isBlank();
    }
}
