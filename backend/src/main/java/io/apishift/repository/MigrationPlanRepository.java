package io.apishift.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.apishift.entity.GeneratedResourceEntity;
import io.apishift.entity.MigrationPlanEntity;
import io.apishift.model.MigrationPlan;
import io.apishift.model.MigrationPrerequisite;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class MigrationPlanRepository implements PanacheRepositoryBase<MigrationPlanEntity, String> {

    @Inject
    ObjectMapper objectMapper;

    public MigrationPlan findPlanById(String planId) {
        MigrationPlanEntity entity = findById(planId);
        return entity != null ? toPlan(entity) : null;
    }

    public List<MigrationPlan> listPlanSummaries() {
        return listAll(Sort.by("createdAt").descending()).stream()
                .map(this::toPlanSummary)
                .collect(Collectors.toList());
    }

    public Optional<List<MigrationPrerequisite>> findPrerequisites(String planId) {
        MigrationPlanEntity entity = findById(planId);
        if (entity == null || entity.prerequisitesJson == null || entity.prerequisitesJson.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(deserializePrerequisites(entity.prerequisitesJson));
    }

    @Transactional
    public void save(MigrationPlan plan) {
        MigrationPlanEntity entity = new MigrationPlanEntity();
        entity.id = plan.id();
        entity.gatewayStrategy = plan.gatewayStrategy();
        try {
            entity.sourceProductsJson = objectMapper.writeValueAsString(plan.sourceProducts());
        } catch (Exception e) {
            entity.sourceProductsJson = "[]";
        }
        entity.aiAnalysis = plan.aiAnalysis();
        entity.createdAt = plan.createdAt();
        entity.catalogInfoYaml = plan.catalogInfoYaml();
        entity.status = plan.status();
        entity.targetClusterId = plan.targetClusterId();
        entity.targetClusterLabel = plan.targetClusterLabel();
        try {
            entity.prerequisitesJson = objectMapper.writeValueAsString(
                    plan.prerequisites() != null ? plan.prerequisites() : List.of());
        } catch (Exception e) {
            entity.prerequisitesJson = "[]";
        }
        try {
            entity.consolidationWarningsJson = objectMapper.writeValueAsString(
                    plan.consolidationWarnings() != null ? plan.consolidationWarnings() : List.of());
        } catch (Exception e) {
            entity.consolidationWarningsJson = "[]";
        }

        List<GeneratedResourceEntity> resourceEntities = new ArrayList<>();
        for (MigrationPlan.GeneratedResource r : plan.resources()) {
            GeneratedResourceEntity re = new GeneratedResourceEntity();
            re.kind = r.kind();
            re.name = r.name();
            re.namespace = r.namespace();
            re.yaml = r.yaml();
            re.plan = entity;
            resourceEntities.add(re);
        }
        entity.resources = resourceEntities;
        persist(entity);
    }

    @Transactional
    public void updateStatus(String planId, String status) {
        MigrationPlanEntity entity = findById(planId);
        if (entity != null) {
            entity.status = status;
            persist(entity);
        }
    }

    private MigrationPlan toPlan(MigrationPlanEntity e) {
        List<String> products = deserializeProducts(e.sourceProductsJson);
        List<MigrationPlan.GeneratedResource> resources = e.resources != null
                ? e.resources.stream()
                .map(r -> new MigrationPlan.GeneratedResource(r.kind, r.name, r.namespace, r.yaml))
                .collect(Collectors.toList())
                : List.of();
        return new MigrationPlan(
                e.id, e.gatewayStrategy, products, resources,
                e.aiAnalysis, e.createdAt, e.catalogInfoYaml, e.status,
                e.targetClusterId, e.targetClusterLabel,
                deserializeStringList(e.consolidationWarningsJson),
                deserializePrerequisites(e.prerequisitesJson)
        );
    }

    /** List view / hub overview: metadata only (avoids loading large generated YAML blobs). */
    private MigrationPlan toPlanSummary(MigrationPlanEntity e) {
        return new MigrationPlan(
                e.id, e.gatewayStrategy, deserializeProducts(e.sourceProductsJson),
                List.of(),
                e.aiAnalysis, e.createdAt, e.catalogInfoYaml, e.status,
                e.targetClusterId, e.targetClusterLabel,
                deserializeStringList(e.consolidationWarningsJson),
                deserializePrerequisites(e.prerequisitesJson)
        );
    }

    private List<String> deserializeProducts(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> deserializeStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            Log.warnf(e, "Failed to deserialize string list JSON");
            return List.of();
        }
    }

    private List<MigrationPrerequisite> deserializePrerequisites(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, MigrationPrerequisite.class));
        } catch (Exception e) {
            Log.warnf(e, "Failed to deserialize prerequisites JSON");
            return List.of();
        }
    }
}
