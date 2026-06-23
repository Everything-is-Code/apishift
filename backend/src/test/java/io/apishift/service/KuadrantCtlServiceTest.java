package io.apishift.service;

import io.apishift.service.support.ReflectionTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KuadrantCtlServiceTest {

    @Test
    void version_withMissingBinary_returnsError() {
        KuadrantCtlService service = new KuadrantCtlService();
        ReflectionTestSupport.inject(service, "kuadrantctlPath", "/nonexistent/kuadrantctl");

        String result = service.version();

        assertTrue(result.startsWith("ERROR:"));
    }

    @Test
    void generateHttpRoute_withMissingBinary_returnsError() {
        KuadrantCtlService service = new KuadrantCtlService();
        ReflectionTestSupport.inject(service, "kuadrantctlPath", "/nonexistent/kuadrantctl");

        String result = service.generateHttpRoute("openapi: 3.0.0\ninfo:\n  title: demo");

        assertTrue(result.startsWith("ERROR:"));
    }

    @Test
    void topology_withMissingBinary_returnsError() {
        KuadrantCtlService service = new KuadrantCtlService();
        ReflectionTestSupport.inject(service, "kuadrantctlPath", "/nonexistent/kuadrantctl");

        String result = service.topology("default");

        assertTrue(result.startsWith("ERROR:"));
    }

    @Test
    void generateAuthPolicy_withMissingBinary_returnsError() {
        KuadrantCtlService service = new KuadrantCtlService();
        ReflectionTestSupport.inject(service, "kuadrantctlPath", "/nonexistent/kuadrantctl");

        String result = service.generateAuthPolicy("openapi: 3.0.0\ninfo:\n  title: demo");

        assertTrue(result.startsWith("ERROR:"));
    }

    @Test
    void generateRateLimitPolicy_withMissingBinary_returnsError() {
        KuadrantCtlService service = new KuadrantCtlService();
        ReflectionTestSupport.inject(service, "kuadrantctlPath", "/nonexistent/kuadrantctl");

        String result = service.generateRateLimitPolicy("openapi: 3.0.0\ninfo:\n  title: demo");

        assertTrue(result.startsWith("ERROR:"));
    }
}
