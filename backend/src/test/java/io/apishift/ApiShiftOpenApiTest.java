package io.apishift;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiShiftOpenApiTest {

    @Test
    void canBeConstructed() {
        assertNotNull(new ApiShiftOpenApi());
    }
}
