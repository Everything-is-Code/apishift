package io.gateforge.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gateforge.service.support.ExportMinimalFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExportBackendIndexTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void load_resolvesByIdAndSystemName() throws Exception {
        Path backendsDir = ExportMinimalFixture.root().resolve("backends");
        ExportBackendIndex index = ExportBackendIndex.load(backendsDir, objectMapper);

        assertEquals("billing_api", index.resolveBackendName(101));
        assertEquals("http://billing.internal", index.privateEndpoint(101));
        assertEquals("http://billing.internal", index.privateEndpointByName("billing_api"));
    }

    @Test
    void load_missingDirectory_returnsEmptyIndex() throws Exception {
        ExportBackendIndex index = ExportBackendIndex.load(Path.of("/nonexistent/backends"), objectMapper);

        assertEquals("backend-42", index.resolveBackendName(42));
        assertNull(index.privateEndpoint(42));
    }
}
