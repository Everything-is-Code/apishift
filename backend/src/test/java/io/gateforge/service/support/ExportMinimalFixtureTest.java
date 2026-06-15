package io.gateforge.service.support;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ExportMinimalFixtureTest {

    @Test
    void root_extractsTarballWithManifestAndProducts() {
        var root = ExportMinimalFixture.root();

        assertTrue(Files.isRegularFile(root.resolve("manifest.json")));
        assertTrue(Files.isDirectory(root.resolve("products")));
        assertTrue(Files.isRegularFile(root.resolve("products/seed_alpha.yaml")));
        assertEquals(root, ExportMinimalFixture.root(), "fixture root should be cached");
    }
}
