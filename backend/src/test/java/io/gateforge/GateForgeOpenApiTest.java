package io.gateforge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GateForgeOpenApiTest {

    @Test
    void canBeConstructed() {
        assertNotNull(new GateForgeOpenApi());
    }
}
