package io.gateforge.service.generator;

import io.gateforge.model.MigrationPlan;
import io.gateforge.model.ThreeScaleProduct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RateLimitResourceGenerator {

    public record DerivedRateLimit(long limit, String window, boolean placeholder) {}

    public static DerivedRateLimit deriveGlobalRateLimit(ThreeScaleProduct product) {
        long maxMinute = 0;
        long maxHour = 0;
        if (product != null && product.applicationPlans() != null) {
            for (ThreeScaleProduct.ApplicationPlan plan : product.applicationPlans()) {
                if (plan.limits() == null) {
                    continue;
                }
                for (ThreeScaleProduct.PlanLimit limit : plan.limits()) {
                    if ("minute".equals(limit.period()) && limit.value() > maxMinute) {
                        maxMinute = limit.value();
                    } else if ("hour".equals(limit.period()) && limit.value() > maxHour) {
                        maxHour = limit.value();
                    }
                }
            }
        }
        if (maxMinute > 0) {
            return new DerivedRateLimit(maxMinute, "1m", false);
        }
        if (maxHour > 0) {
            return new DerivedRateLimit(maxHour, "1h", false);
        }
        return new DerivedRateLimit(100, "60s", true);
    }

    public MigrationPlan.GeneratedResource build(
            String name, String namespace, String routeName, ThreeScaleProduct product,
            DerivedRateLimit derivedRateLimit) {

        String yaml = """
                apiVersion: kuadrant.io/v1
                kind: RateLimitPolicy
                metadata:
                  name: %s
                  namespace: %s
                  labels:
                    app.kubernetes.io/managed-by: gateforge
                    "gateforge.io/product": "%s"
                spec:
                  targetRef:
                    group: gateway.networking.k8s.io
                    kind: HTTPRoute
                    name: %s
                  limits:
                    "global":
                      rates:
                        - limit: %d
                          window: %s
                """.formatted(name, namespace, product.systemName(), routeName,
                derivedRateLimit.limit(), derivedRateLimit.window());

        return new MigrationPlan.GeneratedResource("RateLimitPolicy", name, namespace, yaml);
    }
}
