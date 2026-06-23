package io.apishift.service.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ThreeScaleAdminApiFixtures {

    private ThreeScaleAdminApiFixtures() {}

    public static final String SERVICES_JSON = """
            {
              "services": [
                {
                  "service": {
                    "id": 1,
                    "name": "Demo API",
                    "system_name": "demo-api",
                    "description": "Test service",
                    "deployment_option": "hosted"
                  }
                }
              ]
            }
            """;

    public static final String BACKEND_APIS_JSON = """
            {
              "backend_apis": [
                {
                  "backend_api": {
                    "id": 10,
                    "name": "API Backend",
                    "system_name": "api-backend",
                    "private_endpoint": "http://api-backend.default.svc:8080"
                  }
                }
              ]
            }
            """;

    public static final String EMPTY_SERVICES_JSON = """
            { "services": [] }
            """;

    public static Map<String, Object> demoService() {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("id", 1L);
        service.put("name", "Demo API");
        service.put("system_name", "demo-api");
        service.put("description", "Test service");
        service.put("deployment_option", "hosted");
        return service;
    }

    public static Map<String, Object> otherService() {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("id", 2L);
        service.put("name", "Other API");
        service.put("system_name", "other-api");
        service.put("description", "Second service");
        service.put("deployment_option", "hosted");
        return service;
    }

    public static List<Map<String, Object>> demoMappingRules() {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("http_method", "GET");
        rule.put("pattern", "/");
        rule.put("metric_id", "hits");
        rule.put("delta", 1);
        return List.of(rule);
    }

    public static List<Map<String, Object>> demoBackendUsages() {
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("backend_id", 10L);
        usage.put("path", "/");
        return List.of(usage);
    }

    public static List<Map<String, Object>> demoBackendApis() {
        Map<String, Object> backend = new LinkedHashMap<>();
        backend.put("id", 10L);
        backend.put("name", "API Backend");
        backend.put("system_name", "api-backend");
        backend.put("private_endpoint", "http://api-backend.default.svc:8080");
        backend.put("description", "Backend API");
        return List.of(backend);
    }
}
