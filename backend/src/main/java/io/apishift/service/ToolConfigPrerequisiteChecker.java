package io.apishift.service;

import io.apishift.model.MigrationPrerequisite;
import io.apishift.model.TargetCluster;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ToolConfigPrerequisiteChecker {

    private static final String DEFAULT_CLUSTER_DOMAIN = "apps.cluster.example.com";

    @Inject
    ClusterRegistry clusterRegistry;

    @ConfigProperty(name = "apishift.cluster-domain", defaultValue = DEFAULT_CLUSTER_DOMAIN)
    String clusterDomain;

    public List<MigrationPrerequisite> fromConfig(String targetClusterId) {
        List<MigrationPrerequisite> result = new ArrayList<>();
        String effectiveId = targetClusterId != null && !targetClusterId.isBlank() ? targetClusterId : "local";

        Map<String, Object> access = clusterRegistry.validateAccess(effectiveId);
        boolean connected = Boolean.TRUE.equals(access.get("connected"));

        if (!connected) {
            result.add(toolConfig(
                    "apishift-cluster-api",
                    "Kubernetes API connection",
                    "ApiShift needs a working Kubernetes API client to apply resources to the target cluster.",
                    "unknown"));
        }

        if (!"local".equals(effectiveId)) {
            TargetCluster cluster = clusterRegistry.getCluster(effectiveId);
            if (cluster == null || !cluster.enabled()) {
                result.add(toolConfig(
                        "apishift-target-cluster",
                        "Target cluster configuration",
                        "The selected target cluster must be registered and enabled in ApiShift.",
                        "unknown"));
            }
        }

        if (DEFAULT_CLUSTER_DOMAIN.equals(clusterDomain)) {
            result.add(toolConfig(
                    "apishift-cluster-domain",
                    "Cluster domain",
                    "Set apishift.cluster-domain (CLUSTER_DOMAIN) to your cluster ingress domain before applying Routes and test URLs.",
                    "unknown"));
        }

        return result;
    }

    private static MigrationPrerequisite toolConfig(String id, String title, String description, String status) {
        return new MigrationPrerequisite(
                id, "tool-config", title, description,
                true, false, null, status, 0);
    }
}
