package io.apishift.service;

import io.apishift.model.ThreeScaleProduct;
import io.apishift.service.support.MigrationFixtures;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThreeScaleAuthModeTest {

    @Test
    void fromAuthMap_detectsOidcFromTypeAndIssuerFields() {
        assertEquals(ThreeScaleAuthMode.OIDC, ThreeScaleAuthMode.fromAuthMap(Map.of("type", "oidc")));
        assertEquals(ThreeScaleAuthMode.OIDC,
                ThreeScaleAuthMode.fromAuthMap(Map.of("oidc_issuer_endpoint", "https://issuer.example.com")));
        assertEquals(ThreeScaleAuthMode.API_KEY, ThreeScaleAuthMode.fromAuthMap(Map.of("type", "api_key")));
    }

    @Test
    void parseOidcIssuerEndpoint_stripsEmbeddedCredentials() {
        String cleaned = ThreeScaleAuthMode.parseOidcIssuerEndpoint(
                "https://client:secret@issuer.example.com/realms/demo");

        assertFalse(cleaned.contains("client:secret@"));
        assertTrue(cleaned.contains("issuer.example.com"));
    }

    @Test
    void enrichAuthFromProxy_populatesOidcMetadata() {
        Map<String, Object> auth = new LinkedHashMap<>();
        ThreeScaleAuthMode.enrichAuthFromProxy(auth, Map.of(
                "oidc_issuer_endpoint", "https://client:secret@issuer.example.com",
                "oidc_issuer_type", "authorization_code"));

        assertEquals("oidc", auth.get("type"));
        assertTrue(String.valueOf(auth.get("issuerUrl")).contains("issuer.example.com"));
    }

    @Test
    void introspectionAndBrowserFlowHelpers() {
        Map<String, Object> introspection = Map.of("auth_type", "token_introspection");
        assertTrue(ThreeScaleAuthMode.isTokenIntrospection(introspection));
        assertEquals("https://idp/token/introspect",
                ThreeScaleAuthMode.resolveIntrospectionUrl(Map.of("token_introspection_endpoint", "https://idp/token/introspect")));

        Map<String, Object> browser = Map.of("oidc_issuer_type", "authorization_code");
        assertTrue(ThreeScaleAuthMode.suggestsBrowserOAuthFlow(browser));
    }

    @Test
    void deploymentAndStrategyHints() {
        ThreeScaleProduct tlsProduct = MigrationFixtures.apiKeyProduct();
        ThreeScaleProduct customDeployment = new ThreeScaleProduct(
                tlsProduct.name(), tlsProduct.namespace(), tlsProduct.systemName(), tlsProduct.serviceId(),
                tlsProduct.description(), "custom-ssl", tlsProduct.mappingRules(), tlsProduct.backendUsages(),
                tlsProduct.authentication(), tlsProduct.source(), tlsProduct.backendNamespace(),
                tlsProduct.backendServiceName(), tlsProduct.sourceCluster(),
                tlsProduct.applicationPlans(), tlsProduct.applications());

        assertTrue(ThreeScaleAuthMode.suggestsTlsTermination(customDeployment));
        assertTrue(ThreeScaleAuthMode.suggestsDnsPolicy("dual", MigrationFixtures.apiKeyProduct()));
    }
}
