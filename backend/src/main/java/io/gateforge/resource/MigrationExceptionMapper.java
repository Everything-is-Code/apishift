package io.gateforge.resource;

import io.gateforge.service.export.ExportImportException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class MigrationExceptionMapper implements ExceptionMapper<ExportImportException> {

    @Override
    public Response toResponse(ExportImportException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}
