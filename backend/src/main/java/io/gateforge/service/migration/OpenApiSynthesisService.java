package io.gateforge.service.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.model.ThreeScaleProduct;
import io.gateforge.service.generator.HttpRouteResourceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetches backend OpenAPI documents and synthesizes specs from 3scale mapping rules.
 */
@ApplicationScoped
public class OpenApiSynthesisService {

    private static final Logger LOG = Logger.getLogger(OpenApiSynthesisService.class);

    private static final Map<String, Object> UNAUTHORIZED_RESPONSE = Map.of(
            "description", "Unauthorized",
            "content", Map.of("application/json", Map.of(
                    "example", Map.of("error", "unauthorized", "message", "API key is missing or invalid"))));

    private static final Map<String, Object> NOT_FOUND_RESPONSE = Map.of(
            "description", "Not Found",
            "content", Map.of("application/json", Map.of(
                    "example", Map.of("error", "not_found", "message", "Resource not found"))));

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String fetchFullSpec(String baseUrl) {
        for (String suffix : List.of("/q/openapi", "/openapi.json", "/openapi", "/swagger.json",
                "/q/openapi?format=json", "/v3/api-docs")) {
            try {
                String url = baseUrl.endsWith("/")
                        ? baseUrl.substring(0, baseUrl.length() - 1) + suffix
                        : baseUrl + suffix;

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .header("Accept", "application/json, application/yaml")
                        .GET().build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    continue;
                }

                String body = resp.body().trim();
                if (body.isEmpty()) {
                    continue;
                }

                JsonNode root;
                if (body.startsWith("{")) {
                    root = objectMapper.readTree(body);
                } else {
                    org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = yaml.load(body);
                    root = objectMapper.valueToTree(map);
                }

                if (root.has("paths") && root.get("paths").isObject() && root.get("paths").size() > 0) {
                    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                }
            } catch (Exception e) {
                LOG.debugf("OpenAPI fetch failed for %s%s: %s", baseUrl, suffix, e.getMessage());
            }
        }
        return null;
    }

    public List<ThreeScaleProduct.MappingRule> fetchPaths(String baseUrl) {
        for (String suffix : List.of("/q/openapi", "/openapi.json", "/openapi", "/swagger.json")) {
            try {
                String url = baseUrl.endsWith("/")
                        ? baseUrl.substring(0, baseUrl.length() - 1) + suffix
                        : baseUrl + suffix;

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .header("Accept", "application/json")
                        .GET().build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    continue;
                }

                String body = resp.body().trim();
                JsonNode root;
                if (body.startsWith("{")) {
                    root = objectMapper.readTree(body);
                } else {
                    org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = yaml.load(body);
                    root = objectMapper.valueToTree(map);
                }

                JsonNode paths = root.get("paths");
                if (paths == null || !paths.isObject()) {
                    continue;
                }

                List<ThreeScaleProduct.MappingRule> rules = new ArrayList<>();
                var fields = paths.fields();
                while (fields.hasNext()) {
                    var entry = fields.next();
                    String path = entry.getKey();
                    if (path.equals("/")) {
                        continue;
                    }
                    JsonNode methods = entry.getValue();
                    var methodFields = methods.fields();
                    while (methodFields.hasNext()) {
                        var mEntry = methodFields.next();
                        String method = mEntry.getKey().toUpperCase();
                        if (Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS").contains(method)) {
                            String opId = "";
                            JsonNode opNode = mEntry.getValue();
                            if (opNode.has("operationId")) {
                                opId = opNode.get("operationId").asText();
                            }
                            rules.add(new ThreeScaleProduct.MappingRule(method, path, opId, 1));
                        }
                    }
                }
                if (!rules.isEmpty()) {
                    return rules;
                }

            } catch (Exception e) {
                LOG.debugf("OpenAPI fetch failed for %s: %s", baseUrl + suffix, e.getMessage());
            }
        }
        return List.of();
    }

    public String synthesizeFromMappingRules(ThreeScaleProduct product,
            List<ThreeScaleProduct.MappingRule> rules, String hostname) {
        if (rules == null || rules.isEmpty()) {
            return buildMinimalOpenApi(product, hostname);
        }

        Map<String, Map<String, Object>> paths = new LinkedHashMap<>();

        for (ThreeScaleProduct.MappingRule rule : rules) {
            String path = HttpRouteResourceGenerator.sanitizePath(rule.pattern());
            if (path.contains("{")) {
                path = path.replaceAll("/\\{[^}]+}", "/{id}");
            }

            paths.computeIfAbsent(path, k -> new LinkedHashMap<>());
            Map<String, Object> method = new LinkedHashMap<>();
            method.put("summary", rule.metricRef() != null && !rule.metricRef().isBlank()
                    ? rule.metricRef() : rule.httpMethod() + " " + path);
            method.put("operationId", rule.httpMethod().toLowerCase() + path.replaceAll("[^a-zA-Z0-9]", "_"));

            boolean isCollection = "GET".equalsIgnoreCase(rule.httpMethod()) && !path.contains("{id}");
            Map<String, Object> responses = buildEnrichedResponses(path, product.name(), isCollection);
            method.put("responses", responses);
            paths.get(path).put(rule.httpMethod().toLowerCase(), method);
        }

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.3");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", product.name());
        info.put("description", product.description() != null ? product.description() : "Migrated from 3scale by GateForge");
        info.put("version", "1.0.0");
        spec.put("info", info);
        spec.put("servers", List.of(Map.of("url", "https://" + hostname)));
        spec.put("paths", paths);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);
        } catch (Exception e) {
            LOG.warnf("Failed to serialize synthetic OAS for %s", product.systemName());
            return null;
        }
    }

    private String buildMinimalOpenApi(ThreeScaleProduct product, String hostname) {
        Map<String, Object> okContent = new LinkedHashMap<>();
        okContent.put("schema", Map.of("type", "object", "properties", Map.of(
                "status", Map.of("type", "string"),
                "service", Map.of("type", "string"),
                "timestamp", Map.of("type", "string", "format", "date-time"))));
        okContent.put("example", Map.of("status", "ok", "service", product.name(), "timestamp", "2026-04-22T10:30:00Z"));

        Map<String, Object> okResp = new LinkedHashMap<>();
        okResp.put("description", "OK");
        okResp.put("content", Map.of("application/json", okContent));

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.3");
        spec.put("info", Map.of("title", product.name(), "version", "1.0.0"));
        spec.put("servers", List.of(Map.of("url", "https://" + hostname)));
        spec.put("paths", Map.of("/", Map.of("get", Map.of(
                "summary", "Root endpoint",
                "operationId", "getRoot",
                "responses", Map.of("200", okResp,
                        "401", UNAUTHORIZED_RESPONSE)))));
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildEnrichedResponses(String path, String productName, boolean isCollection) {
        Map<String, Object> example = resolveExamplePayload(path, productName);
        Object body = isCollection ? List.of(example) : example;

        Map<String, Object> okContent = new LinkedHashMap<>();
        okContent.put("schema", Map.of("type", isCollection ? "array" : "object"));
        okContent.put("example", body);

        Map<String, Object> okResp = new LinkedHashMap<>();
        okResp.put("description", "Success");
        okResp.put("content", Map.of("application/json", okContent));

        Map<String, Object> responses = new LinkedHashMap<>();
        responses.put("200", okResp);
        responses.put("401", UNAUTHORIZED_RESPONSE);
        responses.put("404", NOT_FOUND_RESPONSE);
        return responses;
    }

    private Map<String, Object> resolveExamplePayload(String path, String productName) {
        String lp = path.toLowerCase();
        if (lp.contains("account")) {
            return Map.of("accountId", "ACC-001", "holder", "John Doe", "balance", 1500.00,
                    "currency", "USD", "status", "active");
        }
        if (lp.contains("card") || lp.contains("card-issuing")) {
            return Map.of("cardId", "CARD-9876", "last4", "4242", "type", "virtual",
                    "status", "active", "expiresAt", "2028-12");
        }
        if (lp.contains("transaction") || lp.contains("payment")) {
            return Map.of("transactionId", "TXN-5432", "amount", 99.99, "currency", "USD",
                    "status", "completed", "createdAt", "2026-04-22T10:30:00Z");
        }
        if (lp.contains("wallet") || lp.contains("nfl-wallet")) {
            return Map.of("walletId", "W-001", "owner", "user1", "balance", 250.00,
                    "currency", "USD");
        }
        if (lp.contains("user") || lp.contains("customer")) {
            return Map.of("userId", "USR-001", "name", "Jane Smith", "email", "jane@example.com",
                    "status", "active");
        }
        if (lp.contains("product") || lp.contains("catalog")) {
            return Map.of("productId", "PROD-001", "name", "Premium Plan", "price", 29.99,
                    "currency", "USD", "available", true);
        }
        if (lp.contains("order")) {
            return Map.of("orderId", "ORD-001", "total", 149.99, "currency", "USD",
                    "status", "confirmed", "createdAt", "2026-04-22T10:30:00Z");
        }
        return Map.of("id", "resource-001", "name", productName, "status", "ok",
                "timestamp", "2026-04-22T10:30:00Z");
    }
}
