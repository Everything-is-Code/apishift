package io.gateforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.model.ThreeScaleProduct;
import io.gateforge.model.ThreeScaleSource;
import io.gateforge.service.support.ExportMinimalFixture;
import io.gateforge.service.support.ReflectionTestSupport;
import io.gateforge.service.support.RemoteCacheStub;
import io.gateforge.service.support.StubThreeScaleAdminApiClient;
import io.gateforge.service.support.ThreeScaleAdminApiFixtures;
import io.gateforge.service.support.ThreeScaleServiceTestSupport;
import io.gateforge.service.support.ThreeScaleTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThreeScaleServiceTest extends ThreeScaleServiceTestSupport {

    private static final String PRODUCTS_CACHE = "threescale-products";
    private static final String BACKENDS_CACHE = "threescale-backends";
    private static final String CACHE_KEY = "all";

    @Test
    void listProducts_loadsFromAdminApi() {
        ObjectMapper mapper = new ObjectMapper();
        StubThreeScaleAdminApiClient client = new StubThreeScaleAdminApiClient("source-a", mapper);
        client.setServices(List.of(ThreeScaleAdminApiFixtures.demoService()));
        client.setMappingRules(ThreeScaleAdminApiFixtures.demoMappingRules());
        client.setBackendUsages(ThreeScaleAdminApiFixtures.demoBackendUsages());

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withStub("source-a", "Source A", client), cache);

        List<ThreeScaleProduct> products = service.listProducts();

        assertEquals(1, products.size());
        ThreeScaleProduct product = products.get(0);
        assertEquals("demo-api", product.systemName());
        assertEquals(1L, product.serviceId());
        assertEquals("source-a", product.sourceCluster());
        assertEquals(1, product.mappingRules().size());
        assertTrue(product.source().contains("Admin API"));
    }

    @Test
    void listProducts_multiSource_returnsDistinctProducts() {
        ObjectMapper mapper = new ObjectMapper();

        StubThreeScaleAdminApiClient clientA = new StubThreeScaleAdminApiClient("source-a", mapper);
        clientA.setServices(List.of(ThreeScaleAdminApiFixtures.demoService()));

        StubThreeScaleAdminApiClient clientB = new StubThreeScaleAdminApiClient("source-b", mapper);
        clientB.setServices(List.of(ThreeScaleAdminApiFixtures.otherService()));

        ThreeScaleSource sourceA = new ThreeScaleSource(
                "source-a", "A", "https://a.example.com", "token-a", true);
        ThreeScaleSource sourceB = new ThreeScaleSource(
                "source-b", "B", "https://b.example.com", "token-b", true);

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withClients(
                        Map.of("source-a", clientA, "source-b", clientB),
                        Map.of("source-a", sourceA, "source-b", sourceB)),
                cache);

        List<ThreeScaleProduct> products = service.listProducts();

        assertEquals(2, products.size());
        assertTrue(products.stream().anyMatch(p -> "source-a".equals(p.sourceCluster())
                && "demo-api".equals(p.systemName())));
        assertTrue(products.stream().anyMatch(p -> "source-b".equals(p.sourceCluster())
                && "other-api".equals(p.systemName())));
    }

    @Test
    void listProducts_cachesResult() {
        ObjectMapper mapper = new ObjectMapper();
        StubThreeScaleAdminApiClient client = new StubThreeScaleAdminApiClient("source-a", mapper);
        client.setServices(List.of(ThreeScaleAdminApiFixtures.demoService()));

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withStub("source-a", "Source A", client), cache);

        service.listProducts();

        assertTrue(cache.contains(PRODUCTS_CACHE, CACHE_KEY));
    }

    @Test
    void listProducts_secondCallUsesCache() {
        ObjectMapper mapper = new ObjectMapper();
        StubThreeScaleAdminApiClient client = new StubThreeScaleAdminApiClient("source-a", mapper);
        client.setServices(List.of(ThreeScaleAdminApiFixtures.demoService()));

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withStub("source-a", "Source A", client), cache);

        service.listProducts();
        service.listProducts();

        assertEquals(1, client.servicesLoadCount());
    }

    @Test
    void evictDiscoveryCache_forcesReload() {
        ObjectMapper mapper = new ObjectMapper();
        StubThreeScaleAdminApiClient client = new StubThreeScaleAdminApiClient("source-a", mapper);
        client.setServices(List.of(ThreeScaleAdminApiFixtures.demoService()));

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withStub("source-a", "Source A", client), cache);

        service.listProducts();
        service.evictDiscoveryCache();

        assertFalse(cache.contains(PRODUCTS_CACHE, CACHE_KEY));
        service.listProducts();

        assertEquals(2, client.servicesLoadCount());
    }

    @Test
    void listBackendsCombined_mapsAdminApiFields() {
        ObjectMapper mapper = new ObjectMapper();
        StubThreeScaleAdminApiClient client = new StubThreeScaleAdminApiClient("source-a", mapper);
        client.setBackendApis(ThreeScaleAdminApiFixtures.demoBackendApis());

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withStub("source-a", "Source A", client), cache);

        List<Map<String, Object>> backends = service.listBackendsCombined();

        assertEquals(1, backends.size());
        assertEquals("Admin API", backends.get(0).get("source"));
        assertEquals("source-a", backends.get(0).get("sourceCluster"));
        assertEquals("api-backend", backends.get(0).get("systemName"));
        assertEquals("http://api-backend.default.svc:8080", backends.get(0).get("privateEndpoint"));
    }

    @Test
    void refreshDiscovery_returnsCounts() {
        ObjectMapper mapper = new ObjectMapper();
        StubThreeScaleAdminApiClient client = new StubThreeScaleAdminApiClient("source-a", mapper);
        client.setServices(List.of(ThreeScaleAdminApiFixtures.demoService()));
        client.setBackendApis(ThreeScaleAdminApiFixtures.demoBackendApis());

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withStub("source-a", "Source A", client), cache);

        Map<String, Object> result = service.refreshDiscovery();

        assertEquals(1, result.get("productCount"));
        assertEquals(1, result.get("backendCount"));
        assertNotNull(result.get("refreshedAt"));
        assertFalse(result.get("refreshedAt").toString().isBlank());
    }

    @Test
    void getAdminApiStatus_reportsConfiguredSources() {
        ObjectMapper mapper = new ObjectMapper();
        StubThreeScaleAdminApiClient client = new StubThreeScaleAdminApiClient("source-a", mapper);
        client.setServices(List.of(ThreeScaleAdminApiFixtures.demoService()));

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withStub("source-a", "Source A", client), cache);
        service.listProducts();

        Map<String, Object> status = service.getAdminApiStatus();

        assertTrue((Boolean) status.get("configured"));
        assertNotNull(status.get("sources"));
        assertEquals(1, ((List<?>) status.get("sources")).size());
    }

    @Test
    void refreshProductForMigration_enrichesAuthAndApplications() {
        ObjectMapper mapper = new ObjectMapper();
        StubThreeScaleAdminApiClient client = new StubThreeScaleAdminApiClient("source-a", mapper);
        client.setServiceProxy(Map.of("auth_user_key", "user_key", "oidc_issuer_endpoint", "https://issuer.example.com"));
        client.setApplicationPlans(List.of(Map.of(
                "id", 10, "name", "Basic", "system_name", "basic", "state", "published")));
        client.setApplications(List.of(Map.of(
                "id", 99, "name", "demo-app", "user_key", "uk-123", "plan_id", 10)));

        ThreeScaleProduct seed = new ThreeScaleProduct(
                "Demo", "admin-api", "demo-api", 1L, "desc", "standard",
                List.of(), List.of(), Map.of(), "Admin API (source-a)",
                null, null, "source-a", List.of(), List.of());

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withStub("source-a", "Source A", client), cache);

        ThreeScaleProduct refreshed = service.refreshProductForMigration(seed);

        assertEquals(ThreeScaleAuthMode.OIDC, ThreeScaleAuthMode.fromProduct(refreshed));
        assertEquals(1, refreshed.applications().size());
        assertEquals("uk-123", refreshed.applications().get(0).userKey());
    }

    @Test
    void listBackendsCombined_secondCallUsesCache() {
        ObjectMapper mapper = new ObjectMapper();
        StubThreeScaleAdminApiClient client = new StubThreeScaleAdminApiClient("source-a", mapper);
        client.setBackendApis(ThreeScaleAdminApiFixtures.demoBackendApis());

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withStub("source-a", "Source A", client), cache);

        service.listBackendsCombined();
        service.listBackendsCombined();

        assertTrue(cache.contains(BACKENDS_CACHE, CACHE_KEY));
    }

    @Test
    void clearExportOverride_restoresAdminApiProducts() {
        ObjectMapper mapper = new ObjectMapper();
        StubThreeScaleAdminApiClient client = new StubThreeScaleAdminApiClient("source-a", mapper);
        client.setServices(List.of(ThreeScaleAdminApiFixtures.demoService()));

        RemoteCacheStub cache = RemoteCacheStub.create();
        ThreeScaleService service = createService(
                ThreeScaleTestRegistry.withStub("source-a", "Source A", client), cache);
        ReflectionTestSupport.inject(service, "exportParser", new io.gateforge.service.export.ThreeScaleExportParser());

        service.loadFromExport(ExportMinimalFixture.root());
        assertEquals(2, service.listProducts().size());

        service.clearExportOverride();
        assertEquals(1, service.listProducts().size());
    }
}
