package io.apishift.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Map;

@Schema(description = "3scale backend from CRD discovery or Admin API")
public record ThreeScaleBackend(
        String name,
        String namespace,
        Object id,
        String systemName,
        String privateEndpoint,
        String description,
        String source,
        String sourceCluster,
        String createdAt,
        String updatedAt,
        Map<String, Object> spec) {

    @SuppressWarnings("unchecked")
    public static ThreeScaleBackend fromMap(Map<String, Object> m) {
        return new ThreeScaleBackend(
                stringVal(m.get("name")),
                stringVal(m.get("namespace")),
                m.get("id"),
                stringVal(m.get("systemName")),
                stringVal(m.get("privateEndpoint")),
                stringVal(m.get("description")),
                stringVal(m.get("source")),
                stringVal(m.get("sourceCluster")),
                stringVal(m.get("createdAt")),
                stringVal(m.get("updatedAt")),
                m.get("spec") instanceof Map<?, ?> spec ? (Map<String, Object>) spec : null);
    }

    private static String stringVal(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
