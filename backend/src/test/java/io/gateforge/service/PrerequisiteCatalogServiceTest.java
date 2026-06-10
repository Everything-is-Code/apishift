package io.gateforge.service;

import io.gateforge.model.MigrationPlan;
import io.gateforge.model.MigrationPrerequisite;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrerequisiteCatalogServiceTest {

    private final PrerequisiteCatalogService service = new PrerequisiteCatalogService();

    @Test
    void fromPlan_mapsConnectivityAndCorePolicies() {
        List<MigrationPlan.GeneratedResource> resources = List.of(
                resource("Gateway", "gw", """
                        apiVersion: gateway.networking.k8s.io/v1
                        kind: Gateway
                        """),
                resource("AuthPolicy", "auth", """
                        apiVersion: kuadrant.io/v1
                        kind: AuthPolicy
                        """),
                resource("PlanPolicy", "plan", """
                        apiVersion: extensions.kuadrant.io/v1alpha1
                        kind: PlanPolicy
                        """)
        );

        List<MigrationPrerequisite> prerequisites = service.fromPlan(resources, "istio", "kuadrant-system");

        assertTrue(prerequisites.stream().anyMatch(p -> "gateway-api".equals(p.id())));
        assertTrue(prerequisites.stream().anyMatch(p -> "rhcl-core".equals(p.id())));
        assertTrue(prerequisites.stream().anyMatch(p -> "kuadrant-extensions".equals(p.id())));
        prerequisites.forEach(p -> assertEquals("unknown", p.status()));
    }

    @Test
    void fromPlan_includesPortalAndRoute() {
        List<MigrationPlan.GeneratedResource> resources = List.of(
                resource("APIProduct", "product", """
                        apiVersion: devportal.kuadrant.io/v1alpha1
                        kind: APIProduct
                        """),
                resource("Route", "route", """
                        apiVersion: route.openshift.io/v1
                        kind: Route
                        """)
        );

        List<MigrationPrerequisite> prerequisites = service.fromPlan(resources, "istio", "kuadrant-system");

        MigrationPrerequisite portal = prerequisites.stream()
                .filter(p -> "developer-portal".equals(p.id()))
                .findFirst()
                .orElseThrow();
        assertEquals("portal", portal.category());
        assertTrue(portal.optionalTier());
        assertTrue(prerequisites.stream().anyMatch(p -> "openshift-route".equals(p.id())));
    }

    @Test
    void fromPlan_detectsAuthorinoSecrets() {
        List<MigrationPlan.GeneratedResource> resources = List.of(
                resource("Secret", "key", """
                        apiVersion: v1
                        kind: Secret
                        stringData:
                          api_key: abc
                        """)
        );

        List<MigrationPrerequisite> prerequisites = service.fromPlan(resources, "istio", "kuadrant-system");

        assertTrue(prerequisites.stream().anyMatch(p -> "authorino-secrets".equals(p.id())));
    }

    private static MigrationPlan.GeneratedResource resource(String kind, String name, String yaml) {
        return new MigrationPlan.GeneratedResource(kind, name, "ns", yaml);
    }
}
