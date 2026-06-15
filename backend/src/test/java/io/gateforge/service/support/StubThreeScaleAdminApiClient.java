package io.gateforge.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.service.ThreeScaleAdminApiClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StubThreeScaleAdminApiClient extends ThreeScaleAdminApiClient {

    private final AtomicInteger servicesLoadCount = new AtomicInteger();
    private List<Map<String, Object>> services = new ArrayList<>();
    private List<Map<String, Object>> mappingRules = Collections.emptyList();
    private List<Map<String, Object>> backendUsages = Collections.emptyList();
    private List<Map<String, Object>> backendApis = Collections.emptyList();
    private List<Map<String, Object>> applicationPlans = Collections.emptyList();
    private List<Map<String, Object>> applications = Collections.emptyList();
    private Map<String, Object> serviceProxy = Collections.emptyMap();

    public StubThreeScaleAdminApiClient(String sourceId, ObjectMapper objectMapper) {
        super(sourceId, "https://3scale.example.com", "test-token", objectMapper);
    }

    public void setServices(List<Map<String, Object>> services) {
        this.services = new ArrayList<>(services);
    }

    public void setMappingRules(List<Map<String, Object>> mappingRules) {
        this.mappingRules = mappingRules;
    }

    public void setBackendUsages(List<Map<String, Object>> backendUsages) {
        this.backendUsages = backendUsages;
    }

    public void setBackendApis(List<Map<String, Object>> backendApis) {
        this.backendApis = backendApis;
    }

    public int servicesLoadCount() {
        return servicesLoadCount.get();
    }

    @Override
    public List<Map<String, Object>> listServices() {
        servicesLoadCount.incrementAndGet();
        return services;
    }

    @Override
    public List<Map<String, Object>> listMappingRules(long serviceId) {
        return mappingRules;
    }

    @Override
    public List<Map<String, Object>> listBackendUsages(long serviceId) {
        return backendUsages;
    }

    @Override
    public List<Map<String, Object>> listBackendApis() {
        return backendApis;
    }

    @Override
    public List<Map<String, Object>> listApplicationPlans(long serviceId) {
        return applicationPlans;
    }

    @Override
    public List<Map<String, Object>> listApplications(long serviceId) {
        return applications;
    }

    @Override
    public Map<String, Object> getServiceProxy(long serviceId) {
        return serviceProxy;
    }
}
