package io.gateforge.service.generator;

import io.gateforge.service.ThreeScaleAuthMode;
import io.gateforge.service.support.MigrationFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthPolicyResourceGeneratorTest {

    private final AuthPolicyResourceGenerator generator = new AuthPolicyResourceGenerator();

    @Test
    void build_apiKeyMode_emitsApiKeyAuthentication() {
        var product = MigrationFixtures.apiKeyProduct();

        var resource = generator.build("auth-demo", "default", "demo-route", product, ThreeScaleAuthMode.API_KEY);

        assertEquals("AuthPolicy", resource.kind());
        assertTrue(resource.yaml().contains("apikey-auth"));
        assertTrue(resource.yaml().contains("demo-api"));
    }

    @Test
    void build_oidcMode_emitsJwtIssuer() {
        var product = MigrationFixtures.oidcWithoutIssuer();

        var resource = generator.build("auth-oidc", "default", "demo-route", product, ThreeScaleAuthMode.OIDC);

        assertTrue(resource.yaml().contains("oidc-auth"));
        assertTrue(resource.yaml().contains("issuerUrl"));
    }

    @Test
    void build_introspectionMode_emitsOAuth2Introspection() {
        var product = MigrationFixtures.oidcIntrospectionProduct();

        var resource = generator.build("auth-intro", "default", "demo-route", product, ThreeScaleAuthMode.OIDC);

        assertTrue(resource.yaml().contains("introspection-auth"));
        assertTrue(resource.yaml().contains("oauth2Introspection"));
    }
}
