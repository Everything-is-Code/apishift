package io.gateforge.service.generator;

import io.gateforge.model.MigrationPlan;
import io.gateforge.model.ThreeScaleProduct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class HttpRouteResourceGenerator {

    @Inject
    MigrationGeneratorConfig config;

    public MigrationPlan.GeneratedResource build(
            String name, String namespace, String gatewayName,
            ThreeScaleProduct product, List<ThreeScaleProduct.MappingRule> effectiveRules,
            String backendSvcName, List<ResolvedBackend> resolvedBackends) {

        StringBuilder rules = new StringBuilder();

        boolean multiBackend = resolvedBackends.size() > 1;

        if (multiBackend) {
            for (ResolvedBackend rb : resolvedBackends) {
                String pathPrefix = rb.path().equals("/") ? "/" : rb.path();
                rules.append("        - matches:\n");
                rules.append("            - path:\n");
                rules.append("                type: PathPrefix\n");
                rules.append("                value: ").append(pathPrefix).append("\n");
                rules.append("          backendRefs:\n");
                rules.append("            - name: ").append(rb.svcName()).append("\n");
                rules.append("              port: 8080\n");
            }
        } else if (effectiveRules.isEmpty()) {
            rules.append("""
                        - matches:
                            - path:
                                type: PathPrefix
                                value: /
                          backendRefs:
                            - name: %s
                              port: 8080
                      """.formatted(backendSvcName));
        } else {
            Set<String> prefixes = new LinkedHashSet<>();
            for (ThreeScaleProduct.MappingRule r : effectiveRules) {
                String p = sanitizePath(r.pattern());
                if (p.contains("{")) p = p.replaceAll("/\\{[^}]+}.*", "");
                prefixes.add(p.equals("/") ? "/" : p);
            }

            if (prefixes.size() > 16) {
                prefixes = Set.of("/");
            }

            for (String prefix : prefixes) {
                rules.append("        - matches:\n");
                rules.append("            - path:\n");
                rules.append("                type: PathPrefix\n");
                rules.append("                value: ").append(prefix).append("\n");
                rules.append("          backendRefs:\n");
                rules.append("            - name: ").append(backendSvcName).append("\n");
                rules.append("              port: 8080\n");
            }
        }

        String hostname = product.systemName() + "." + config.clusterDomain();

        String yaml = """
                apiVersion: gateway.networking.k8s.io/v1
                kind: HTTPRoute
                metadata:
                  name: %s
                  namespace: %s
                  labels:
                    app.kubernetes.io/managed-by: gateforge
                    "gateforge.io/product": "%s"
                spec:
                  hostnames:
                    - %s
                  parentRefs:
                    - name: %s
                      namespace: %s
                  rules:
                %s""".formatted(name, namespace, product.systemName(),
                hostname, gatewayName, config.gatewayNamespace(), rules.toString());

        return new MigrationPlan.GeneratedResource("HTTPRoute", name, namespace, yaml);
    }

    public static String sanitizePath(String pattern) {
        if (pattern == null || pattern.isBlank()) return "/";
        String p = pattern.replaceAll("\\$$", "");
        if (!p.startsWith("/")) p = "/" + p;
        if (p.isEmpty()) p = "/";
        return p;
    }
}
