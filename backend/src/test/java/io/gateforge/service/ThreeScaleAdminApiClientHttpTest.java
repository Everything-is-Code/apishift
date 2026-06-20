package io.gateforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.service.support.ThreeScaleAdminApiFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThreeScaleAdminApiClientHttpTest {

    private HttpServer server;
    private int port;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void listServices_fetchesPaginatedJson() {
        server.createContext("/admin/api/services.json", exchange -> writeJson(exchange, ThreeScaleAdminApiFixtures.SERVICES_JSON));

        ThreeScaleAdminApiClient client = clientForBaseUrl();
        List<Map<String, Object>> services = client.listServices();

        assertEquals(1, services.size());
        assertEquals("demo-api", services.get(0).get("system_name"));
    }

    @Test
    void getServiceProxy_returnsProxyDocument() {
        server.createContext("/admin/api/services/1/proxy.json", exchange -> writeJson(exchange,
                "{\"proxy\":{\"auth_user_key\":\"user_key\",\"credentials_location\":\"headers\"}}"));

        ThreeScaleAdminApiClient client = clientForBaseUrl();
        Map<String, Object> proxy = client.getServiceProxy(1);

        assertEquals("user_key", proxy.get("auth_user_key"));
    }

    @Test
    void ping_whenReachable_doesNotThrow() {
        server.createContext("/admin/api/services.json", exchange -> writeJson(exchange, ThreeScaleAdminApiFixtures.SERVICES_JSON));

        ThreeScaleAdminApiClient client = clientForBaseUrl();

        assertDoesNotThrow(client::ping);
    }

    @Test
    void ping_whenUnreachable_throws() {
        ThreeScaleAdminApiClient client = new ThreeScaleAdminApiClient(
                "offline", "http://127.0.0.1:1", "token", objectMapper);

        assertThrows(Exception.class, client::ping);
    }

    @Test
    void listBackendApis_fetchesPaginatedJson() {
        server.createContext("/admin/api/backend_apis.json", exchange -> writeJson(exchange, ThreeScaleAdminApiFixtures.BACKEND_APIS_JSON));

        ThreeScaleAdminApiClient client = clientForBaseUrl();
        List<Map<String, Object>> backends = client.listBackendApis();

        assertEquals(1, backends.size());
        assertEquals("api-backend", backends.get(0).get("system_name"));
    }

    @Test
    void listMappingRules_fetchesPaginatedJson() {
        server.createContext("/admin/api/services/1/proxy/mapping_rules.json", exchange -> writeJson(exchange,
                "{\"mapping_rules\":[{\"mapping_rule\":{\"http_method\":\"GET\",\"pattern\":\"/\",\"metric_id\":\"hits\",\"delta\":1}}]}"));

        ThreeScaleAdminApiClient client = clientForBaseUrl();
        List<Map<String, Object>> rules = client.listMappingRules(1);

        assertEquals(1, rules.size());
        assertEquals("GET", rules.get(0).get("http_method"));
    }

    private ThreeScaleAdminApiClient clientForBaseUrl() {
        return new ThreeScaleAdminApiClient("lab", "http://127.0.0.1:" + port, "secret-token", objectMapper);
    }

    private static void writeJson(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
