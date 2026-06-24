package io.apishift.service;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.apishift.entity.MigrationPlanEntity;
import io.apishift.model.ClusterReadiness;
import io.apishift.model.MigrationPrerequisite;
import io.apishift.repository.MigrationPlanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;

import java.util.*;

@ApplicationScoped
public class ClusterReadinessService {

    @Inject
    ClusterRegistry clusterRegistry;

    @Inject
    PrerequisiteCatalogService catalogService;

    @Inject
    MigrationPlanRepository migrationPlanRepository;

    public List<MigrationPrerequisite> enrich(List<MigrationPrerequisite> prerequisites, String targetClusterId) {
        if (prerequisites == null || prerequisites.isEmpty()) {
            return List.of();
        }

        String effectiveId = targetClusterId != null && !targetClusterId.isBlank() ? targetClusterId : "local";
        Map<String, Object> access = clusterRegistry.validateAccess(effectiveId);
        boolean connected = Boolean.TRUE.equals(access.get("connected"));

        if (!connected) {
            return prerequisites.stream()
                    .map(p -> p.withStatus("unknown"))
                    .toList();
        }

        KubernetesClient client = clusterRegistry.getClient(effectiveId);
        Map<String, String> crdStatuses = probeCrds(client, prerequisites);

        List<MigrationPrerequisite> enriched = new ArrayList<>();
        for (MigrationPrerequisite p : prerequisites) {
            enriched.add(enrichOne(p, crdStatuses, prerequisites));
        }
        return enriched;
    }

    public ClusterReadiness probe(String targetClusterId, String planId) {
        String effectiveId = targetClusterId != null && !targetClusterId.isBlank() ? targetClusterId : "local";
        Map<String, Object> access = clusterRegistry.validateAccess(effectiveId);
        boolean connected = Boolean.TRUE.equals(access.get("connected"));
        String connectionStatus = connected ? "satisfied" : "unknown";

        List<MigrationPrerequisite> prerequisites;
        if (planId != null && !planId.isBlank()) {
            Optional<List<MigrationPrerequisite>> storedPrereqs = migrationPlanRepository.findPrerequisites(planId);
            if (storedPrereqs.isPresent()) {
                prerequisites = enrich(storedPrereqs.get(), effectiveId);
            } else {
                prerequisites = baselinePrerequisites(clientFor(effectiveId), connected);
            }
        } else {
            prerequisites = baselinePrerequisites(clientFor(effectiveId), connected);
        }

        return new ClusterReadiness(connected, effectiveId, connectionStatus, prerequisites);
    }

    private List<MigrationPrerequisite> baselinePrerequisites(KubernetesClient client, boolean connected) {
        List<String> ids = List.of(
                "gateway-api", "rhcl-core", "kuadrant-extensions", "developer-portal", "openshift-route");
        List<MigrationPrerequisite> baseline = new ArrayList<>();
        for (String id : ids) {
            MigrationPrerequisite def = catalogService.definitionForId(id, 0, "unknown");
            if (def != null) baseline.add(def);
        }
        if (!connected) {
            return baseline;
        }
        Map<String, String> statuses = probeCrds(client, baseline);
        List<MigrationPrerequisite> result = new ArrayList<>();
        for (MigrationPrerequisite p : baseline) {
            result.add(enrichOne(p, statuses, baseline));
        }
        return result;
    }

    private KubernetesClient clientFor(String clusterId) {
        return clusterRegistry.getClient(clusterId);
    }

    private MigrationPrerequisite enrichOne(
            MigrationPrerequisite p,
            Map<String, String> crdStatuses,
            List<MigrationPrerequisite> all) {

        if ("tool-config".equals(p.category())) {
            return p;
        }

        if ("authorino-secrets".equals(p.id())) {
            String coreStatus = crdStatuses.getOrDefault("rhcl-core", "unknown");
            if ("satisfied".equals(coreStatus)) {
                return p.withStatus("satisfied");
            }
            return p.withStatus("unknown");
        }

        if (catalogService.probeableIds().contains(p.id())) {
            return p.withStatus(crdStatuses.getOrDefault(p.id(), "unknown"));
        }

        return p;
    }

    private Map<String, String> probeCrds(KubernetesClient client, List<MigrationPrerequisite> prerequisites) {
        Map<String, String> statuses = new HashMap<>();
        Set<String> idsToProbe = new LinkedHashSet<>();
        for (MigrationPrerequisite p : prerequisites) {
            if (catalogService.probeableIds().contains(p.id())) {
                idsToProbe.add(p.id());
            }
        }

        for (String id : idsToProbe) {
            statuses.put(id, probeCrd(client, id));
        }
        return statuses;
    }

    private String probeCrd(KubernetesClient client, String prerequisiteId) {
        Optional<String> crdName = catalogService.crdNameForId(prerequisiteId);
        if (crdName.isEmpty()) {
            return "unknown";
        }
        try {
            CustomResourceDefinition crd = client.apiextensions().v1()
                    .customResourceDefinitions()
                    .withName(crdName.get())
                    .get();
            return crd != null ? "satisfied" : "missing";
        } catch (Exception e) {
            if (isRbacOrConnectionError(e)) {
                Log.debugf("CRD probe for %s returned unknown: %s", prerequisiteId, e.getMessage());
                return "unknown";
            }
            Log.debugf("CRD probe for %s returned missing: %s", prerequisiteId, e.getMessage());
            return "missing";
        }
    }

    private static boolean isRbacOrConnectionError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase(Locale.ROOT) : "";
        return msg.contains("forbidden")
                || msg.contains("unauthorized")
                || msg.contains("connection")
                || msg.contains("timeout")
                || msg.contains("refused");
    }

    @SuppressWarnings("unchecked")
    private List<MigrationPrerequisite> deserializePrerequisites(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json,
                    mapper.getTypeFactory().constructCollectionType(List.class, MigrationPrerequisite.class));
        } catch (Exception e) {
            Log.warnf(e, "Failed to deserialize prerequisites JSON");
            return List.of();
        }
    }
}
