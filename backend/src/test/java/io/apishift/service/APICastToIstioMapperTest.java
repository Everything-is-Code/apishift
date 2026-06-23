package io.apishift.service;

import io.apishift.model.APICastConfig;
import io.apishift.model.MigrationPlan;
import io.apishift.service.support.APICastMapperFixtures;
import io.apishift.service.support.APICastTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class APICastToIstioMapperTest {

    private final APICastToIstioMapper mapper = APICastTestSupport.createMapper();

    @Test
    void mapGateways_stagingAndProduction() {
        APICastConfig config = APICastMapperFixtures.gatewaysOnly("api-1", "ns-a");

        List<MigrationPlan.GeneratedResource> resources = mapper.mapAPICastToIstio(config);

        assertEquals(2, resources.size());
        assertTrue(resources.stream().allMatch(r -> "Gateway".equals(r.kind())));
        assertTrue(resources.stream().anyMatch(r -> r.name().equals("api-1-staging-gateway")));
        assertTrue(resources.stream().anyMatch(r -> r.name().equals("api-1-production-gateway")));
    }

    @Test
    void mapGateways_stagingOnly_omitsProduction() {
        APICastConfig config = APICastMapperFixtures.withCustomPolicy("api-2", "ns-b", "rate-limit");

        List<MigrationPlan.GeneratedResource> resources = mapper.mapAPICastToIstio(config);

        assertEquals(2, resources.size());
        assertTrue(resources.stream().anyMatch(r -> "Gateway".equals(r.kind())));
        assertFalse(resources.stream().anyMatch(r -> r.name().contains("production")));
        assertTrue(resources.stream().anyMatch(r -> "EnvoyFilter".equals(r.kind())));
    }

    @Test
    void mapCustomPolicy_generatesEnvoyFilter() {
        APICastConfig config = APICastMapperFixtures.withCustomPolicy("api-3", "ns-c", "custom-rl");

        List<MigrationPlan.GeneratedResource> resources = mapper.mapAPICastToIstio(config);

        MigrationPlan.GeneratedResource envoy = resources.stream()
                .filter(r -> "EnvoyFilter".equals(r.kind()))
                .findFirst()
                .orElseThrow();
        assertEquals("api-3-custom-rl", envoy.name());
        assertTrue(envoy.yaml().contains("custom-rl"));
        assertTrue(envoy.yaml().contains("policy-secret"));
    }

    @Test
    void mapTlsVerify_generatesDestinationRule() {
        APICastConfig config = APICastMapperFixtures.fullFeatured("api-4", "ns-d");

        List<MigrationPlan.GeneratedResource> resources = mapper.mapAPICastToIstio(config);

        MigrationPlan.GeneratedResource dr = resources.stream()
                .filter(r -> "DestinationRule".equals(r.kind()))
                .findFirst()
                .orElseThrow();
        assertEquals("api-4-tls", dr.name());
        assertTrue(dr.yaml().contains("mode: SIMPLE"));
    }

    @Test
    void mapOpenTracing_generatesTelemetry() {
        APICastConfig config = APICastMapperFixtures.fullFeatured("api-5", "ns-e");

        List<MigrationPlan.GeneratedResource> resources = mapper.mapAPICastToIstio(config);

        MigrationPlan.GeneratedResource telemetry = resources.stream()
                .filter(r -> "Telemetry".equals(r.kind()))
                .findFirst()
                .orElseThrow();
        assertEquals("api-5-telemetry", telemetry.name());
        assertTrue(telemetry.yaml().contains("randomSamplingPercentage: 100.0"));
    }

    @Test
    void mapService_generatesServiceEntry() {
        APICastConfig config = APICastMapperFixtures.fullFeatured("api-6", "ns-f");

        List<MigrationPlan.GeneratedResource> resources = mapper.mapAPICastToIstio(config);

        MigrationPlan.GeneratedResource se = resources.stream()
                .filter(r -> "ServiceEntry".equals(r.kind()))
                .findFirst()
                .orElseThrow();
        assertEquals("api-6-external", se.name());
        assertTrue(se.yaml().contains("number: 8080"));
    }

    @Test
    void mapMinimal_onlyGateways() {
        APICastConfig config = APICastMapperFixtures.gatewaysOnly("api-7", "ns-g");

        List<MigrationPlan.GeneratedResource> resources = mapper.mapAPICastToIstio(config);

        assertEquals(2, resources.size());
        assertTrue(resources.stream().allMatch(r -> "Gateway".equals(r.kind())));
    }

    @Test
    void mapGateway_usesConfiguredDomainAndClass() {
        APICastConfig config = APICastMapperFixtures.gatewaysOnly("api-8", "ns-h");

        List<MigrationPlan.GeneratedResource> resources = mapper.mapAPICastToIstio(config);

        String yaml = resources.get(0).yaml();
        assertTrue(yaml.contains("gatewayClassName: istio"));
        assertTrue(yaml.contains("hostname: \"*.apps.example.com\""));
        assertTrue(yaml.contains("apishift.io/original-replicas: \"2\""));
    }

    @Test
    void mapMultiple_batchMapping() {
        APICastConfig first = APICastMapperFixtures.gatewaysOnly("api-9", "ns-i");
        APICastConfig second = APICastMapperFixtures.productionOnly("api-10", "ns-j");

        List<List<MigrationPlan.GeneratedResource>> batches =
                mapper.mapMultipleAPICasts(List.of(first, second));

        assertEquals(2, batches.size());
        assertEquals(2, batches.get(0).size());
        assertEquals(1, batches.get(1).size());
        assertEquals("api-10-production-gateway", batches.get(1).get(0).name());
    }
}
