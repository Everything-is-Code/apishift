package io.gateforge.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Result of evicting discovery cache and reloading 3scale data")
public record ThreeScaleRefreshResult(
        int productCount,
        int backendCount,
        String refreshedAt) {

    public static ThreeScaleRefreshResult fromMap(Map<String, Object> m) {
        return new ThreeScaleRefreshResult(
                numberVal(m.get("productCount")),
                numberVal(m.get("backendCount")),
                String.valueOf(m.getOrDefault("refreshedAt", "")));
    }

    private static int numberVal(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
