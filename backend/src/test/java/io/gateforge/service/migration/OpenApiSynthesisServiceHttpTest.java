package io.gateforge.service.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiSynthesisServiceHttpTest {

    private HttpServer server;
    private int port;
    private OpenApiSynthesisService service;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();

        service = new OpenApiSynthesisService();
        ReflectionTestSupport.inject(service, "objectMapper", new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchFullSpec_readsOpenApiDocument() {
        server.createContext("/openapi.json", exchange -> writeJson(exchange, """
                {"openapi":"3.0.3","paths":{"/health":{"get":{"responses":{"200":{"description":"ok"}}}}}}
                """));

        String spec = service.fetchFullSpec("http://127.0.0.1:" + port);

        assertNotNull(spec);
        assertTrue(spec.contains("/health"));
    }

    @Test
    void fetchPaths_extractsMappingRules() {
        server.createContext("/openapi.json", exchange -> writeJson(exchange, """
                {"openapi":"3.0.3","paths":{
                  "/items":{"get":{"operationId":"listItems"}},
                  "/items/{id}":{"delete":{"operationId":"deleteItem"}}
                }}
                """));

        List<io.gateforge.model.ThreeScaleProduct.MappingRule> rules =
                service.fetchPaths("http://127.0.0.1:" + port);

        assertEquals(2, rules.size());
        assertTrue(rules.stream().anyMatch(r -> "GET".equals(r.httpMethod()) && "/items".equals(r.pattern())));
        assertTrue(rules.stream().anyMatch(r -> "DELETE".equals(r.httpMethod()) && "/items/{id}".equals(r.pattern())));
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
