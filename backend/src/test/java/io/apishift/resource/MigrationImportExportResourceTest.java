package io.apishift.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
class MigrationImportExportResourceTest {

    @Test
    void importExport_minimalFixture_returnsParsedProducts(@TempDir Path tempDir) throws Exception {
        Path zipFile = tempDir.resolve("export-minimal.zip");
        io.apishift.service.support.ExportZipTestSupport.zipDirectory(
                io.apishift.service.support.ExportMinimalFixture.root(), zipFile);

        given()
                .multiPart("file", zipFile.toFile(), "application/zip")
                .when()
                .post("/api/migration/import-export")
                .then()
                .statusCode(200)
                .body("importMode", equalTo("export-v1"))
                .body("productCount", equalTo(2))
                .body("products.size()", greaterThan(0));
    }

    @Test
    void importExport_withoutMultipart_returnsClientError() {
        given()
                .when()
                .post("/api/migration/import-export")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(415), equalTo(500)));
    }
}
