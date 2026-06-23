package io.apishift.service;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.apishift.model.APICastConfig;
import io.apishift.service.support.APICastDiscoveryFixtures;
import io.apishift.service.support.APICastKubernetesStub;
import io.apishift.service.support.APICastTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class APICastDiscoveryServiceTest {

    @Test
    void discoverAll_disabled_returnsEmpty() {
        KubernetesClient client = APICastKubernetesStub.create(APICastKubernetesStub.Options.empty());
        APICastDiscoveryService service = APICastTestSupport.createDiscovery(client, false);

        assertTrue(service.discoverAllAPIManagers().isEmpty());
    }

    @Test
    void discoverAll_filtersNonSelfManaged() {
        GenericKubernetesResource nonApicast = APICastDiscoveryFixtures.apiManager(
                "plain", "ns-1", null, true);
        KubernetesClient client = APICastKubernetesStub.create(new APICastKubernetesStub.Options(
                List.of(nonApicast), List.of(), List.of()));
        APICastDiscoveryService service = APICastTestSupport.createDiscovery(client, true);

        assertTrue(service.discoverAllAPIManagers().isEmpty());
    }

    @Test
    void discoverAll_filtersNotReady() {
        GenericKubernetesResource notReady = APICastDiscoveryFixtures.apiManager(
                "not-ready", "ns-2", Map.of("stagingSpec", Map.of("replicas", 1)), false);
        KubernetesClient client = APICastKubernetesStub.create(new APICastKubernetesStub.Options(
                List.of(notReady), List.of(), List.of()));
        APICastDiscoveryService service = APICastTestSupport.createDiscovery(client, true);

        assertTrue(service.discoverAllAPIManagers().isEmpty());
    }

    @Test
    void discoverAll_enrichesConfig() {
        Map<String, Object> apicastSpec = APICastDiscoveryFixtures.richApicastSpec();
        GenericKubernetesResource ready = APICastDiscoveryFixtures.apiManager(
                "enriched", "ns-3", apicastSpec, true);
        KubernetesClient client = APICastKubernetesStub.create(new APICastKubernetesStub.Options(
                List.of(ready), List.of(), List.of()));
        APICastDiscoveryService service = APICastTestSupport.createDiscovery(client, true);

        List<APICastConfig> configs = service.discoverAllAPIManagers();

        assertEquals(1, configs.size());
        APICastConfig config = configs.get(0);
        assertEquals("enriched", config.apiManagerName());
        assertEquals("ns-3", config.namespace());
        assertEquals(2, config.stagingSpec().replicas());
        assertEquals("100m", config.stagingSpec().cpu());
        assertEquals(3, config.productionSpec().replicas());
        assertEquals(1, config.customPolicies().size());
        assertEquals("policy-a", config.customPolicies().get(0).name());
        assertTrue(config.tls().verify());
        assertTrue(config.openTracing().enabled());
        assertEquals("jaeger", config.openTracing().tracingLibrary());
    }

    @Test
    void discoverByNamespace_scopesToNamespace() {
        GenericKubernetesResource inNs = APICastDiscoveryFixtures.apiManager(
                "in-ns", "target-ns",
                Map.of("stagingSpec", Map.of("replicas", 1)), true);
        GenericKubernetesResource otherNs = APICastDiscoveryFixtures.apiManager(
                "other", "other-ns",
                Map.of("stagingSpec", Map.of("replicas", 1)), true);
        KubernetesClient client = APICastKubernetesStub.create(new APICastKubernetesStub.Options(
                List.of(inNs, otherNs), List.of(), List.of()));
        APICastDiscoveryService service = APICastTestSupport.createDiscovery(client, true);

        List<APICastConfig> configs = service.discoverByNamespace("target-ns");

        assertEquals(1, configs.size());
        assertEquals("in-ns", configs.get(0).apiManagerName());
    }

    @Test
    void discoverByName_found() {
        GenericKubernetesResource manager = APICastDiscoveryFixtures.apiManager(
                "found-me", "ns-found",
                Map.of("productionSpec", Map.of("replicas", 2)), true);
        KubernetesClient client = APICastKubernetesStub.create(new APICastKubernetesStub.Options(
                List.of(manager), List.of(), List.of()));
        APICastDiscoveryService service = APICastTestSupport.createDiscovery(client, true);

        APICastConfig config = service.discoverByName("found-me", "ns-found");

        assertNotNull(config);
        assertEquals("found-me", config.apiManagerName());
        assertEquals(2, config.productionSpec().replicas());
    }

    @Test
    void discoverByName_notFound() {
        KubernetesClient client = APICastKubernetesStub.create(APICastKubernetesStub.Options.empty());
        APICastDiscoveryService service = APICastTestSupport.createDiscovery(client, true);

        assertNull(service.discoverByName("missing", "ns-missing"));
    }

    @Test
    void discoverByName_notReady() {
        GenericKubernetesResource notReady = APICastDiscoveryFixtures.apiManager(
                "pending", "ns-pending",
                Map.of("stagingSpec", Map.of("replicas", 1)), false);
        KubernetesClient client = APICastKubernetesStub.create(new APICastKubernetesStub.Options(
                List.of(notReady), List.of(), List.of()));
        APICastDiscoveryService service = APICastTestSupport.createDiscovery(client, true);

        assertNull(service.discoverByName("pending", "ns-pending"));
    }

    @Test
    void discoverByName_countsProductsAndPods() {
        GenericKubernetesResource manager = APICastDiscoveryFixtures.apiManager(
                "counted", "ns-count",
                Map.of("stagingSpec", Map.of("replicas", 1)), true);
        GenericKubernetesResource productA = APICastDiscoveryFixtures.product("prod-a", "ns-count");
        GenericKubernetesResource productB = APICastDiscoveryFixtures.product("prod-b", "ns-count");
        KubernetesClient client = APICastKubernetesStub.create(new APICastKubernetesStub.Options(
                List.of(manager),
                List.of(productA, productB),
                List.of(
                        APICastDiscoveryFixtures.apicastPod("counted", "ns-count"),
                        APICastDiscoveryFixtures.apicastPod("other", "ns-count"))));
        APICastDiscoveryService service = APICastTestSupport.createDiscovery(client, true);

        APICastConfig config = service.discoverByName("counted", "ns-count");

        assertNotNull(config);
        assertEquals(2, config.productsCount());
        assertEquals(1, config.apiCastPods());
    }
}
