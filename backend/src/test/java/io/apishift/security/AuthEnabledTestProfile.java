package io.apishift.security;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class AuthEnabledTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.ofEntries(
                Map.entry("apishift.auth.enabled", "true"),
                Map.entry("apishift.auth.protect-reads", "false"),
                Map.entry("quarkus.oidc.tenant-enabled", "false"),
                Map.entry("quarkus.http.auth.basic", "true"),
                Map.entry("quarkus.security.users.embedded.enabled", "true"),
                Map.entry("quarkus.security.users.embedded.plain-text", "true"),
                Map.entry("quarkus.security.users.embedded.users.admin", "admin"),
                Map.entry("quarkus.security.users.embedded.roles.admin", "admin,operator,viewer"),
                Map.entry("quarkus.security.users.embedded.users.operator", "operator"),
                Map.entry("quarkus.security.users.embedded.roles.operator", "operator,viewer"),
                Map.entry("quarkus.security.users.embedded.users.viewer", "viewer"),
                Map.entry("quarkus.security.users.embedded.roles.viewer", "viewer"));
    }
}
