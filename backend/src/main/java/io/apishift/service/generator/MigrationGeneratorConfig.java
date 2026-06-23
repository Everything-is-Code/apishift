package io.apishift.service.generator;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MigrationGeneratorConfig {

    @ConfigProperty(name = "apishift.connectivity-link.gateway-class-name", defaultValue = "istio")
    String gatewayClassName;

    @ConfigProperty(name = "apishift.connectivity-link.target-namespace", defaultValue = "kuadrant-system")
    String gatewayNamespace;

    @ConfigProperty(name = "apishift.cluster-domain", defaultValue = "apps.cluster.example.com")
    String clusterDomain;

    public String gatewayClassName() {
        return gatewayClassName;
    }

    public String gatewayNamespace() {
        return gatewayNamespace;
    }

    public String clusterDomain() {
        return clusterDomain;
    }
}
