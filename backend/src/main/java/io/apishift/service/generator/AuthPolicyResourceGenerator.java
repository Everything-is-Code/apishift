package io.apishift.service.generator;

import io.apishift.model.MigrationPlan;
import io.apishift.model.ThreeScaleProduct;
import io.apishift.service.ThreeScaleAuthMode;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthPolicyResourceGenerator {

    public MigrationPlan.GeneratedResource build(
            String name, String namespace, String routeName, ThreeScaleProduct product,
            ThreeScaleAuthMode authMode) {

        String authSection;
        if (ThreeScaleAuthMode.isTokenIntrospection(product.authentication())) {
            String endpoint = ThreeScaleAuthMode.resolveIntrospectionUrl(product.authentication());
            if (endpoint == null || endpoint.isBlank()) {
                endpoint = "https://sso.example.com/realms/api/protocol/openid-connect/token/introspect";
            }
            String credentialsSecret = product.systemName() + "-introspection-credentials";
            authSection = """
                          authentication:
                            "introspection-auth":
                              oauth2Introspection:
                                endpoint: %s
                                credentialsRef:
                                  name: %s
                      """.formatted(endpoint, credentialsSecret);
        } else if (authMode == ThreeScaleAuthMode.OIDC) {
            String issuer = ThreeScaleAuthMode.resolveJwtIssuerUrl(product.authentication());
            if (issuer == null || issuer.isBlank()) {
                issuer = "https://sso.example.com/realms/api";
            }
            authSection = """
                          authentication:
                            "oidc-auth":
                              jwt:
                                issuerUrl: %s
                      """.formatted(issuer);
        } else {
            authSection = """
                          authentication:
                            "apikey-auth":
                              apiKey:
                                selector:
                                  matchLabels:
                                    app: %s
                                allNamespaces: false
                      """.formatted(product.systemName());
        }

        String yaml = """
                apiVersion: kuadrant.io/v1
                kind: AuthPolicy
                metadata:
                  name: %s
                  namespace: %s
                  labels:
                    app.kubernetes.io/managed-by: apishift
                    "ApiShift.io/product": "%s"
                spec:
                  targetRef:
                    group: gateway.networking.k8s.io
                    kind: HTTPRoute
                    name: %s
                  rules:
                %s""".formatted(name, namespace, product.systemName(), routeName, authSection);

        return new MigrationPlan.GeneratedResource("AuthPolicy", name, namespace, yaml);
    }
}
