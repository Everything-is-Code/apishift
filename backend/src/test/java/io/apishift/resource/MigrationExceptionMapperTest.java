package io.apishift.resource;

import io.apishift.service.export.ExportImportException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MigrationExceptionMapperTest {

  private final MigrationExceptionMapper mapper = new MigrationExceptionMapper();

  @Test
  void mapsExportImportExceptionToBadRequest() {
    Response response = mapper.toResponse(new ExportImportException("Only .zip export archives are supported"));

    assertEquals(400, response.getStatus());
    assertNotNull(response.getEntity());
  }
}
