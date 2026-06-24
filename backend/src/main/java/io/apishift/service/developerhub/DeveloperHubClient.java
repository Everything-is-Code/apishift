package io.apishift.service.developerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.apishift.model.MigrationPlan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.logging.Log;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.*;

/**
 * HTTP client for Red Hat Developer Hub catalog and scaffolder integration.
 */
@ApplicationScoped
public class DeveloperHubClient {

    @ConfigProperty(name = "apishift.developer-hub.scaffolder-url", defaultValue = "")
    Optional<String> scaffolderUrl;

    @ConfigProperty(name = "apishift.developer-hub.scaffolder-token", defaultValue = "")
    Optional<String> scaffolderToken;

    @ConfigProperty(name = "apishift.cluster-domain", defaultValue = "apps.cluster.example.com")
    String clusterDomain;

    @ConfigProperty(name = "apishift.developer-hub.component-suffix", defaultValue = "-product")
    String componentSuffix;

    @ConfigProperty(name = "apishift.developer-hub.enabled", defaultValue = "false")
    boolean developerHubEnabled;

    @ConfigProperty(name = "apishift.developer-hub.url", defaultValue = "none")
    String developerHubUrl;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isActive() {
        return developerHubEnabled
                && developerHubUrl != null
                && !developerHubUrl.isBlank()
                && !"none".equalsIgnoreCase(developerHubUrl.trim());
    }

    public void notifyPlanApplied(MigrationPlan plan, String planId) {
        if (!isActive()) {
            return;
        }
        postMigrationEvent(plan, planId);
        registerCatalogEntities(plan);
        unregister3ScaleEntities(plan);
    }

    public void confirmRegistration(MigrationPlan plan, String planId, String componentYaml) {
        for (String productName : plan.sourceProducts()) {
            String sysName = toSystemName(productName);
            String compName = sysName.endsWith(componentSuffix) ? sysName : sysName + componentSuffix;
            String routeName = sysName + "-route";
            String namespace = plan.resources().stream()
                    .filter(r -> "HTTPRoute".equals(r.kind()) && r.name().equals(routeName))
                    .findFirst()
                    .map(MigrationPlan.GeneratedResource::namespace)
                    .orElse("default");

            Map<String, Object> templateValues = new LinkedHashMap<>(Map.of(
                    "planId", planId,
                    "productName", sysName,
                    "componentName", compName,
                    "namespace", namespace,
                    "owner", "group:default/3scale",
                    "clusterDomain", clusterDomain
            ));
            if (componentYaml != null && !componentYaml.isBlank()) {
                templateValues.put("componentYaml", componentYaml);
            }
            triggerScaffolderTemplate("apishift-register-component", templateValues);
        }
    }

    public void unregisterComponents(MigrationPlan plan) {
        for (String productName : plan.sourceProducts()) {
            String sysName = toSystemName(productName);
            String compName = sysName.endsWith(componentSuffix) ? sysName : sysName + componentSuffix;
            try {
                triggerScaffolderTemplate("apishift-unregister-component", Map.of("componentName", compName));
            } catch (Exception e) {
                Log.warnf(e, "Failed to unregister component %s", compName);
            }
        }
    }

    private void postMigrationEvent(MigrationPlan plan, String planId) {
        try {
            String baseUrl = normalizeBaseUrl(developerHubUrl);
            String url = baseUrl + "/api/catalog/migration-event";

            List<Map<String, String>> resourcesPayload = new ArrayList<>();
            for (MigrationPlan.GeneratedResource r : plan.resources()) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("kind", r.kind());
                entry.put("name", r.name());
                entry.put("namespace", r.namespace() != null ? r.namespace() : "");
                resourcesPayload.add(entry);
            }

            for (String productName : plan.sourceProducts()) {
                String sysName = toSystemName(productName);
                String routeName = sysName + "-route";
                String ns = plan.resources().stream()
                        .filter(r -> "HTTPRoute".equals(r.kind()) && routeName.equals(r.name()))
                        .findFirst()
                        .map(MigrationPlan.GeneratedResource::namespace)
                        .orElse("default");

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("products", List.of(sysName));
                payload.put("namespace", ns);
                payload.put("planId", planId);
                payload.put("resources", resourcesPayload);

                HttpResponse<String> resp = sendJsonPost(url, payload);
                Log.infof("Developer Hub migration-event POST for %s → HTTP %d", sysName, resp.statusCode());
                if (resp.statusCode() >= 400) {
                    Log.warnf("Developer Hub migration-event error for %s: %s", sysName, resp.body());
                }
            }
        } catch (Exception e) {
            Log.warnf(e, "Failed to POST migration-event to Developer Hub");
        }
    }

    private void registerCatalogEntities(MigrationPlan plan) {
        String catalogYaml = plan.catalogInfoYaml();
        if (catalogYaml == null || catalogYaml.isBlank()) {
            Log.info("No catalog-info YAML in plan, skipping catalog registration");
            return;
        }
        try {
            String baseUrl = normalizeBaseUrl(developerHubUrl);
            String locationUrl = baseUrl + "/api/catalog/locations";
            String ApiShiftBaseUrl = "https://apishift-apishift." + clusterDomain
                    + "/api/migration/plans/" + plan.id();

            for (String productName : plan.sourceProducts()) {
                String sysName = toSystemName(productName);
                String catalogEndpoint = ApiShiftBaseUrl + "/catalog-info/" + sysName;

                Map<String, Object> locationPayload = new LinkedHashMap<>();
                locationPayload.put("type", "url");
                locationPayload.put("target", catalogEndpoint);

                HttpResponse<String> resp = sendJsonPost(locationUrl, locationPayload);
                Log.infof("Catalog location POST for %s → HTTP %d", sysName, resp.statusCode());
                if (resp.statusCode() >= 400) {
                    Log.warnf("Catalog location error for %s: %s", sysName, resp.body());
                }
            }
        } catch (Exception e) {
            Log.warnf(e, "Failed to register catalog entities");
        }
    }

    private void unregister3ScaleEntities(MigrationPlan plan) {
        try {
            String baseUrl = normalizeBaseUrl(developerHubUrl);
            for (String productName : plan.sourceProducts()) {
                unregister3ScaleEntity(toSystemName(productName), baseUrl);
            }
        } catch (Exception e) {
            Log.warnf(e, "Failed to unregister 3scale entities");
        }
    }

    @SuppressWarnings("unchecked")
    private void unregister3ScaleEntity(String sysName, String baseUrl) {
        try {
            String entitiesUrl = baseUrl + "/api/catalog/entities/by-query?"
                    + "filter=kind=API,metadata.name=" + sysName;

            HttpResponse<String> resp = sendAuthorizedGet(entitiesUrl, Duration.ofSeconds(15));
            if (resp.statusCode() != 200) {
                Log.warnf("Could not query 3scale entity '%s': HTTP %d", sysName, resp.statusCode());
                return;
            }

            List<?> entities = objectMapper.readValue(resp.body(), List.class);
            for (Object entity : entities) {
                Map<String, Object> e = (Map<String, Object>) entity;
                Map<String, Object> metadata = (Map<String, Object>) e.get("metadata");
                Map<String, Object> annotations = metadata != null
                        ? (Map<String, Object>) metadata.get("annotations")
                        : null;
                if (annotations == null) {
                    continue;
                }

                String originLocation = String.valueOf(
                        annotations.getOrDefault("backstage.io/managed-by-origin-location", ""));
                if (!originLocation.contains("3scale")) {
                    continue;
                }

                String uid = String.valueOf(metadata.get("uid"));
                String locationRef = String.valueOf(
                        annotations.getOrDefault("backstage.io/origin-location-ref", ""));

                Log.infof("Unregistering 3scale entity '%s' (uid=%s, origin=%s)", sysName, uid, originLocation);
                if (locationRef != null && !locationRef.isBlank()) {
                    deleteLocationByRef(baseUrl, locationRef);
                } else {
                    deleteEntityByUid(baseUrl, uid);
                }
            }
        } catch (Exception e) {
            Log.warnf(e, "Error unregistering 3scale entity '%s'", sysName);
        }
    }

    private void deleteLocationByRef(String baseUrl, String locationRef) {
        try {
            String url = baseUrl + "/api/catalog/locations/by-query?filter=target=" + locationRef;
            HttpResponse<String> resp = sendAuthorizedGet(url, Duration.ofSeconds(15));
            if (resp.statusCode() == 200) {
                List<?> locations = objectMapper.readValue(resp.body(), List.class);
                for (Object loc : locations) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> l = (Map<String, Object>) loc;
                    String locId = String.valueOf(l.get("id"));
                    HttpRequest delReq = HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + "/api/catalog/locations/" + locId))
                            .timeout(Duration.ofSeconds(15))
                            .DELETE()
                            .build();
                    httpClient.send(delReq, HttpResponse.BodyHandlers.ofString());
                    Log.infof("Deleted 3scale location %s", locId);
                }
            }
        } catch (Exception e) {
            Log.warnf(e, "Error deleting location ref '%s'", locationRef);
        }
    }

    private void deleteEntityByUid(String baseUrl, String uid) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/catalog/entities/by-uid/" + uid))
                    .timeout(Duration.ofSeconds(15));
            addAuthHeader(reqBuilder);
            HttpResponse<String> resp = httpClient.send(reqBuilder.DELETE().build(), HttpResponse.BodyHandlers.ofString());
            Log.infof("Deleted 3scale entity uid=%s → HTTP %d", uid, resp.statusCode());
        } catch (Exception e) {
            Log.warnf(e, "Error deleting entity uid '%s'", uid);
        }
    }

    private void triggerScaffolderTemplate(String templateName, Map<String, Object> values) {
        if (scaffolderUrl.isEmpty() || scaffolderUrl.get().isBlank()) {
            Log.info("Scaffolder URL not configured, skipping template trigger");
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "templateRef", "template:default/" + templateName,
                    "values", values
            );

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(scaffolderUrl.get()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json");
            addAuthHeader(reqBuilder);

            HttpResponse<String> resp = httpClient.send(
                    reqBuilder.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload))).build(),
                    HttpResponse.BodyHandlers.ofString());
            Log.infof("Scaffolder API response: %d — %s", resp.statusCode(), resp.body());

            if (resp.statusCode() >= 400) {
                throw new WebApplicationException(
                        "Developer Hub Scaffolder API returned HTTP " + resp.statusCode() + ": " + resp.body(),
                        resp.statusCode());
            }
        } catch (HttpTimeoutException e) {
            String msg = "Developer Hub Scaffolder API timed out after 30s. The Component may not have been registered. "
                    + "Check the Scaffolder tasks in Developer Hub or retry using POST /api/migration/plans/{id}/confirm-registration.";
            Log.errorf(e, "%s", msg);
            throw new WebApplicationException(msg, 504);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            String msg = "Failed to trigger Scaffolder template: " + e.getMessage();
            Log.errorf(e, "%s", msg);
            throw new WebApplicationException(msg, 502);
        }
    }

    private HttpResponse<String> sendJsonPost(String url, Map<String, Object> payload) throws Exception {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json");
        addAuthHeader(reqBuilder);
        return httpClient.send(
                reqBuilder.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload))).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendAuthorizedGet(String url, Duration timeout) throws Exception {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .GET();
        addAuthHeader(reqBuilder);
        return httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void addAuthHeader(HttpRequest.Builder reqBuilder) {
        if (scaffolderToken.isPresent() && !scaffolderToken.get().isBlank()) {
            reqBuilder.header("Authorization", "Bearer " + scaffolderToken.get());
        }
    }

    static String normalizeBaseUrl(String url) {
        String baseUrl = url.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    static String toSystemName(String productName) {
        return productName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }

    /** Test-only constructor with explicit configuration. */
    DeveloperHubClient(
            boolean enabled,
            String hubUrl,
            Optional<String> scaffolderUrl,
            Optional<String> scaffolderToken,
            String clusterDomain,
            String componentSuffix,
            ObjectMapper objectMapper) {
        this.developerHubEnabled = enabled;
        this.developerHubUrl = hubUrl;
        this.scaffolderUrl = scaffolderUrl;
        this.scaffolderToken = scaffolderToken;
        this.clusterDomain = clusterDomain;
        this.componentSuffix = componentSuffix;
        this.objectMapper = objectMapper;
    }

    DeveloperHubClient() {
    }
}
