package io.apishift.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Aggregate 3scale Admin API and discovery status")
public record ThreeScaleAdminStatus(
        boolean crdDiscoveryEnabled,
        List<ThreeScaleSourceStatus> sources,
        boolean configured,
        Boolean reachable,
        Integer productCount,
        Integer backendApiCount,
        Integer activeDocsCount) {

    @SuppressWarnings("unchecked")
    public static ThreeScaleAdminStatus fromMap(Map<String, Object> m) {
        List<ThreeScaleSourceStatus> sources = List.of();
        if (m.get("sources") instanceof List<?> raw) {
            sources = raw.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> ThreeScaleSourceStatus.fromMap((Map<String, Object>) item))
                    .toList();
        }
        return new ThreeScaleAdminStatus(
                Boolean.TRUE.equals(m.get("crdDiscoveryEnabled")),
                sources,
                Boolean.TRUE.equals(m.get("configured")),
                m.get("reachable") instanceof Boolean b ? b : null,
                m.get("productCount") instanceof Number n ? n.intValue() : null,
                m.get("backendApiCount") instanceof Number n ? n.intValue() : null,
                m.get("activeDocsCount") instanceof Number n ? n.intValue() : null);
    }
}
