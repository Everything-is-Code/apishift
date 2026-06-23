package io.apishift.service.support;

import io.apishift.model.APICastConfig;
import io.apishift.model.APICastConfig.*;

import java.util.List;
import java.util.Map;

public final class APICastMapperFixtures {

    private APICastMapperFixtures() {}

    public static APICastConfig gatewaysOnly(String name, String namespace) {
        return base(name, namespace,
                new APICastDeploymentSpec(2, "100m", "128Mi"),
                new APICastDeploymentSpec(3, "200m", "256Mi"),
                List.of(),
                new TLSConfig(false, 0, null),
                null,
                new OpenTracingConfig(false, null, null));
    }

    public static APICastConfig productionOnly(String name, String namespace) {
        return base(name, namespace,
                null,
                new APICastDeploymentSpec(3, "200m", "256Mi"),
                List.of(),
                new TLSConfig(false, 0, null),
                null,
                new OpenTracingConfig(false, null, null));
    }

    public static APICastConfig withCustomPolicy(String name, String namespace, String policyName) {
        return base(name, namespace,
                new APICastDeploymentSpec(1, null, null),
                null,
                List.of(new CustomPolicy(policyName, "policy-secret", "1.0", "LUA")),
                new TLSConfig(false, 0, null),
                null,
                new OpenTracingConfig(false, null, null));
    }

    public static APICastConfig fullFeatured(String name, String namespace) {
        return base(name, namespace,
                new APICastDeploymentSpec(2, "100m", "128Mi"),
                new APICastDeploymentSpec(3, "200m", "256Mi"),
                List.of(new CustomPolicy("rate-limit", "rl-secret", "1.0", "LUA")),
                new TLSConfig(true, 2, null),
                new ServiceExposureConfig("ClusterIP", 8080, 8080),
                new OpenTracingConfig(true, "jaeger", "http://otel:4317"));
    }

    private static APICastConfig base(
            String name,
            String namespace,
            APICastDeploymentSpec staging,
            APICastDeploymentSpec production,
            List<CustomPolicy> policies,
            TLSConfig tls,
            ServiceExposureConfig service,
            OpenTracingConfig tracing) {
        return new APICastConfig(
                name,
                namespace,
                "ready",
                staging,
                production,
                policies,
                tls,
                service,
                tracing,
                List.of(),
                Map.of(),
                0,
                0);
    }
}
