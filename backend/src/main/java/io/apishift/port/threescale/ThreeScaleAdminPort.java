package io.apishift.port.threescale;

import java.util.List;
import java.util.Map;

/**
 * Outbound port for a single 3scale Admin API source.
 * {@link io.apishift.service.ThreeScaleAdminApiClient} is the HTTP adapter.
 */
public interface ThreeScaleAdminPort {

    String getSourceId();

    boolean isConfigured();

    void ping() throws Exception;

    List<Map<String, Object>> listServices();

    List<Map<String, Object>> listBackendApis();

    List<Map<String, Object>> listActiveDocs();

    List<Map<String, Object>> listMappingRules(long serviceId);

    List<Map<String, Object>> listBackendUsages(long serviceId);

    List<Map<String, Object>> listApplicationPlans(long serviceId);

    List<Map<String, Object>> listPlanLimits(long planId);

    List<Map<String, Object>> listApplications(long serviceId);

    Map<String, Object> getServiceProxy(long serviceId);
}
