package io.apishift.service.generator;

import io.apishift.model.MigrationPlan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GatewayResourceGenerator {

    @Inject
    MigrationGeneratorConfig config;

    public MigrationPlan.GeneratedResource build(String name, String label) {
        String yaml = """
                apiVersion: gateway.networking.k8s.io/v1
                kind: Gateway
                metadata:
                  name: %s
                  namespace: %s
                  annotations:
                    networking.istio.io/service-type: ClusterIP
                  labels:
                    "apishift.io/type": "%s"
                    app.kubernetes.io/managed-by: apishift
                spec:
                  gatewayClassName: %s
                  listeners:
                    - name: http
                      port: 80
                      protocol: HTTP
                      hostname: "*.%s"
                      allowedRoutes:
                        namespaces:
                          from: All
                """.formatted(name, config.gatewayNamespace(), label, config.gatewayClassName(), config.clusterDomain());
        return new MigrationPlan.GeneratedResource("Gateway", name, config.gatewayNamespace(), yaml);
    }
}
