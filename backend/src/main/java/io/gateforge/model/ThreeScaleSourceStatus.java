package io.gateforge.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Reachability status for a configured 3scale Admin API source")
public record ThreeScaleSourceStatus(
        String id,
        String label,
        String adminUrl,
        boolean configured,
        boolean enabled,
        Boolean reachable,
        String error) {

    public static ThreeScaleSourceStatus fromMap(Map<String, Object> m) {
        return new ThreeScaleSourceStatus(
                stringVal(m.get("id")),
                stringVal(m.get("label")),
                stringVal(m.get("adminUrl")),
                Boolean.TRUE.equals(m.get("configured")),
                Boolean.TRUE.equals(m.get("enabled")),
                m.get("reachable") instanceof Boolean b ? b : null,
                stringVal(m.get("error")));
    }

    private static String stringVal(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
