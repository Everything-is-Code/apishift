package io.apishift.service;

import io.apishift.model.ThreeScaleProduct;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Product-level authentication mode for 3scale (mutually exclusive: API Key or OIDC).
 */
public enum ThreeScaleAuthMode {
    API_KEY,
    OIDC;

    public static ThreeScaleAuthMode fromProduct(ThreeScaleProduct product) {
        return fromAuthMap(product != null ? product.authentication() : null);
    }

    public static ThreeScaleAuthMode fromAuthMap(Map<String, Object> auth) {
        if (auth == null || auth.isEmpty()) {
            return API_KEY;
        }
        String type = String.valueOf(auth.getOrDefault("type", "")).trim();
        if ("oidc".equalsIgnoreCase(type) || "openid_connect".equalsIgnoreCase(type)) {
            return OIDC;
        }
        String issuerEndpoint = String.valueOf(auth.getOrDefault("oidc_issuer_endpoint", "")).trim();
        if (!issuerEndpoint.isBlank()) {
            return OIDC;
        }
        String issuerUrl = String.valueOf(auth.getOrDefault("issuerUrl", "")).trim();
        if (!issuerUrl.isBlank()) {
            return OIDC;
        }
        String backendVersion = String.valueOf(auth.getOrDefault("backend_version", "")).trim();
        if ("oidc".equalsIgnoreCase(backendVersion)) {
            return OIDC;
        }
        return API_KEY;
    }

    /**
     * Resolves the JWT issuer URL for Authorino from 3scale proxy auth metadata.
     */
    public static String resolveJwtIssuerUrl(Map<String, Object> auth) {
        if (auth == null || auth.isEmpty()) {
            return null;
        }
        String issuerUrl = String.valueOf(auth.getOrDefault("issuerUrl", "")).trim();
        if (!issuerUrl.isBlank()) {
            return issuerUrl;
        }
        String raw = String.valueOf(auth.getOrDefault("oidc_issuer_endpoint", "")).trim();
        if (!raw.isBlank()) {
            return parseOidcIssuerEndpoint(raw);
        }
        String legacy = String.valueOf(auth.getOrDefault("issuerEndpoint", "")).trim();
        return legacy.isBlank() ? null : legacy;
    }

    /**
     * Strips {@code https://client:secret@host/...} credentials embedded in 3scale OIDC issuer URLs.
     */
    public static String parseOidcIssuerEndpoint(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        try {
            URI uri = URI.create(trimmed);
            if (uri.getScheme() == null) {
                return trimmed;
            }
            if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
                int port = uri.getPort();
                URI clean = new URI(uri.getScheme(), null, uri.getHost(), port, uri.getPath(), uri.getQuery(), uri.getFragment());
                return clean.toString();
            }
            return uri.toString();
        } catch (Exception e) {
            int at = trimmed.indexOf('@');
            if (at > 0 && trimmed.contains("://")) {
                int schemeEnd = trimmed.indexOf("://") + 3;
                int credEnd = trimmed.indexOf('@', schemeEnd);
                if (credEnd > schemeEnd) {
                    return trimmed.substring(0, schemeEnd) + trimmed.substring(credEnd + 1);
                }
            }
            return trimmed;
        }
    }

    public static void enrichAuthFromProxy(Map<String, Object> auth, Map<String, Object> proxy) {
        if (auth == null || proxy == null || proxy.isEmpty()) {
            return;
        }
        auth.put("credentials_location", proxy.getOrDefault("credentials_location", ""));
        auth.put("auth_app_key", proxy.getOrDefault("auth_app_key", ""));
        auth.put("auth_app_id", proxy.getOrDefault("auth_app_id", ""));
        auth.put("auth_user_key", proxy.getOrDefault("auth_user_key", ""));

        Object oidcIssuer = proxy.get("oidc_issuer_endpoint");
        if (oidcIssuer != null && !String.valueOf(oidcIssuer).isBlank()) {
            String raw = String.valueOf(oidcIssuer).trim();
            auth.put("oidc_issuer_endpoint", raw);
            auth.put("issuerUrl", parseOidcIssuerEndpoint(raw));
            auth.put("type", "oidc");
        }
        Object oidcType = proxy.get("oidc_issuer_type");
        if (oidcType != null && !String.valueOf(oidcType).isBlank()) {
            auth.put("oidc_issuer_type", String.valueOf(oidcType));
        }
    }

    public static void markOidcFromBackendVersion(Map<String, Object> auth, String backendVersion) {
        if (auth == null || backendVersion == null) {
            return;
        }
        auth.put("backend_version", backendVersion);
        if ("oidc".equalsIgnoreCase(backendVersion.trim())) {
            auth.put("type", "oidc");
        }
    }

    public static boolean isTokenIntrospection(Map<String, Object> auth) {
        if (auth == null || auth.isEmpty()) {
            return false;
        }
        String authType = String.valueOf(auth.getOrDefault("auth_type", auth.getOrDefault("type", ""))).trim();
        if (authType.toLowerCase().contains("introspection")) {
            return true;
        }
        String credentialsLocation = String.valueOf(auth.getOrDefault("credentials_location", "")).trim();
        return credentialsLocation.toLowerCase().contains("introspection");
    }

    public static String resolveIntrospectionUrl(Map<String, Object> auth) {
        if (auth == null || auth.isEmpty()) {
            return null;
        }
        for (String key : List.of("token_introspection_endpoint", "oidc_introspection_endpoint", "introspection_endpoint")) {
            String value = String.valueOf(auth.getOrDefault(key, "")).trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public static boolean suggestsBrowserOAuthFlow(Map<String, Object> auth) {
        if (auth == null || auth.isEmpty()) {
            return false;
        }
        String issuerType = String.valueOf(auth.getOrDefault("oidc_issuer_type", "")).trim();
        if (issuerType.isBlank()) {
            return false;
        }
        String normalized = issuerType.toLowerCase();
        return normalized.contains("browser") || normalized.contains("authorization_code");
    }

    public static boolean suggestsTlsTermination(ThreeScaleProduct product) {
        if (product == null || product.deploymentOption() == null) {
            return false;
        }
        String deployment = product.deploymentOption().toLowerCase();
        return deployment.contains("custom") || deployment.contains("ssl") || deployment.contains("tls");
    }

    public static boolean suggestsDnsPolicy(String gatewayStrategy, ThreeScaleProduct product) {
        return "dual".equals(gatewayStrategy);
    }
}
