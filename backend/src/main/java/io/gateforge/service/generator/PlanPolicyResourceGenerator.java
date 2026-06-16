package io.gateforge.service.generator;

import io.gateforge.model.MigrationPlan;
import io.gateforge.model.ThreeScaleProduct;
import io.gateforge.service.ThreeScaleAuthMode;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PlanPolicyResourceGenerator {

    public MigrationPlan.GeneratedResource build(
            String name, String namespace, String routeName, ThreeScaleProduct product,
            ThreeScaleAuthMode authMode) {

        return authMode == ThreeScaleAuthMode.OIDC
                ? buildOidc(name, namespace, routeName, product)
                : buildApiKey(name, namespace, routeName, product);
    }

    private MigrationPlan.GeneratedResource buildApiKey(
            String name, String namespace, String routeName, ThreeScaleProduct product) {

        StringBuilder plans = new StringBuilder();
        for (ThreeScaleProduct.ApplicationPlan plan : product.applicationPlans()) {
            if (!"published".equalsIgnoreCase(plan.state()) && !"hidden".equalsIgnoreCase(plan.state())) continue;
            plans.append("    - tier: \"").append(plan.systemName()).append("\"\n");
            plans.append("      predicate: |\n");
            plans.append("        has(auth.identity) && auth.identity.metadata.annotations[\"secret.kuadrant.io/plan-id\"] == \"")
                    .append(escapePredicateLiteral(plan.systemName())).append("\"\n");
            appendPlanLimits(plans, plan);
        }

        return wrapYaml(name, namespace, routeName, product.systemName(), plans.toString());
    }

    private MigrationPlan.GeneratedResource buildOidc(
            String name, String namespace, String routeName, ThreeScaleProduct product) {

        StringBuilder plans = new StringBuilder();
        List<ThreeScaleProduct.Application> apps = product.applications() != null
                ? product.applications() : List.of();

        for (ThreeScaleProduct.ApplicationPlan plan : product.applicationPlans()) {
            if (!"published".equalsIgnoreCase(plan.state()) && !"hidden".equalsIgnoreCase(plan.state())) continue;

            List<String> clientClauses = new ArrayList<>();
            for (ThreeScaleProduct.Application app : apps) {
                String planTier = app.planSystemName().isBlank() ? "default" : app.planSystemName();
                if (!plan.systemName().equals(planTier)) continue;
                String clientId = app.applicationId();
                if (clientId == null || clientId.isBlank()) continue;
                clientClauses.add("auth.identity.metadata.annotations[\"clientID\"] == \""
                        + escapePredicateLiteral(clientId) + "\"");
            }

            plans.append("    - tier: \"").append(plan.systemName()).append("\"\n");
            plans.append("      predicate: |\n");
            if (clientClauses.isEmpty()) {
                plans.append("        false\n");
            } else if (clientClauses.size() == 1) {
                plans.append("        has(auth.identity) && ").append(clientClauses.get(0)).append("\n");
            } else {
                plans.append("        has(auth.identity) && (")
                        .append(String.join(" || ", clientClauses))
                        .append(")\n");
            }
            appendPlanLimits(plans, plan);
        }

        return wrapYaml(name, namespace, routeName, product.systemName(), plans.toString());
    }

    private static MigrationPlan.GeneratedResource wrapYaml(
            String name, String namespace, String routeName, String systemName, String plansBody) {

        String yaml = """
                apiVersion: extensions.kuadrant.io/v1alpha1
                kind: PlanPolicy
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
                  plans:
                %s""".formatted(name, namespace, systemName, routeName, plansBody);

        return new MigrationPlan.GeneratedResource("PlanPolicy", name, namespace, yaml);
    }

    private static void appendPlanLimits(StringBuilder plans, ThreeScaleProduct.ApplicationPlan plan) {
        plans.append("      limits:\n");
        for (ThreeScaleProduct.PlanLimit limit : plan.limits()) {
            switch (limit.period()) {
                case "day" -> plans.append("        daily: ").append(limit.value()).append("\n");
                case "month" -> plans.append("        monthly: ").append(limit.value()).append("\n");
                case "week" -> plans.append("        weekly: ").append(limit.value()).append("\n");
                case "year", "eternity" -> plans.append("        yearly: ").append(limit.value()).append("\n");
                case "hour" -> {
                    plans.append("        custom:\n");
                    plans.append("          - limit: ").append(limit.value()).append("\n");
                    plans.append("            window: \"1h\"\n");
                }
                case "minute" -> {
                    plans.append("        custom:\n");
                    plans.append("          - limit: ").append(limit.value()).append("\n");
                    plans.append("            window: \"1m\"\n");
                }
            }
        }
        if (plan.limits().isEmpty()) {
            plans.append("        daily: 1000\n");
        }
    }

    private static String escapePredicateLiteral(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
