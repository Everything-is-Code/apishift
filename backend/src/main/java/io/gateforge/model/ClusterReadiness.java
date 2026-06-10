package io.gateforge.model;

import java.util.List;

public record ClusterReadiness(
    boolean clusterConnected,
    String targetClusterId,
    String connectionStatus,
    List<MigrationPrerequisite> prerequisites
) {}
