package io.apishift.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "3scale Admin API source (public view — credentials never exposed)")
public record ThreeScaleSourceView(
        String id,
        String label,
        String adminUrl,
        boolean enabled,
        boolean credentialConfigured) {

    public static ThreeScaleSourceView from(ThreeScaleSource source) {
        return new ThreeScaleSourceView(
                source.id(),
                source.label(),
                source.adminUrl(),
                source.enabled(),
                hasCredential(source.accessToken()));
    }

    static boolean hasCredential(String accessToken) {
        return accessToken != null
                && !accessToken.isBlank()
                && !"none".equals(accessToken);
    }
}
