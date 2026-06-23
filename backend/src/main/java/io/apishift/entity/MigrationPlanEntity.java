package io.apishift.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "migration_plans")
public class MigrationPlanEntity extends PanacheEntityBase {

    @Id
    public String id;

    @Column(name = "gateway_strategy")
    public String gatewayStrategy;

    @Column(name = "source_products", columnDefinition = "TEXT")
    public String sourceProductsJson;

    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    public String aiAnalysis;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "catalog_info_yaml", columnDefinition = "TEXT")
    public String catalogInfoYaml;

    public String status;

    @Column(name = "target_cluster_id")
    public String targetClusterId;

    @Column(name = "target_cluster_label")
    public String targetClusterLabel;

    @Column(name = "prerequisites_json", columnDefinition = "TEXT")
    public String prerequisitesJson;

    @Column(name = "consolidation_warnings_json", columnDefinition = "TEXT")
    public String consolidationWarningsJson;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<GeneratedResourceEntity> resources;
}
