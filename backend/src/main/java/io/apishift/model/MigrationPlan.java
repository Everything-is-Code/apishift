package io.apishift.model;

import java.time.Instant;
import java.util.List;

public record MigrationPlan(
    String id,
    String gatewayStrategy,
    List<String> sourceProducts,
    List<GeneratedResource> resources,
    String aiAnalysis,
    Instant createdAt,
    String catalogInfoYaml,
    String status,
    String targetClusterId,
    String targetClusterLabel,
    List<String> consolidationWarnings,
    List<MigrationPrerequisite> prerequisites
) {
    public MigrationPlan(String id, String gatewayStrategy, List<String> sourceProducts,
                         List<GeneratedResource> resources, String aiAnalysis, Instant createdAt,
                         String catalogInfoYaml, String status) {
        this(id, gatewayStrategy, sourceProducts, resources, aiAnalysis, createdAt,
             catalogInfoYaml, status, "local", "Local (in-cluster)", List.of(), List.of());
    }

    public MigrationPlan(String id, String gatewayStrategy, List<String> sourceProducts,
                         List<GeneratedResource> resources, String aiAnalysis, Instant createdAt,
                         String catalogInfoYaml, String status, String targetClusterId,
                         String targetClusterLabel, List<String> consolidationWarnings) {
        this(id, gatewayStrategy, sourceProducts, resources, aiAnalysis, createdAt,
             catalogInfoYaml, status, targetClusterId, targetClusterLabel,
             consolidationWarnings, List.of());
    }

    public record GeneratedResource(String kind, String name, String namespace, String yaml) {}
}
