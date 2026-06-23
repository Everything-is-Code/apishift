package io.apishift.service.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ExportBackendIndex {

    public record BackendRecord(long id, String systemName, String name, String privateEndpoint) {}

    private final Map<Long, BackendRecord> byId = new HashMap<>();
    private final Map<String, BackendRecord> bySystemName = new HashMap<>();

    public String resolveBackendName(long backendId) {
        BackendRecord record = byId.get(backendId);
        if (record != null && record.systemName() != null && !record.systemName().isBlank()) {
            return record.systemName();
        }
        return "backend-" + backendId;
    }

    public String privateEndpoint(long backendId) {
        BackendRecord record = byId.get(backendId);
        return record != null ? record.privateEndpoint() : null;
    }

    public String privateEndpointByName(String systemName) {
        BackendRecord record = bySystemName.get(systemName);
        return record != null ? record.privateEndpoint() : null;
    }

    public static ExportBackendIndex load(Path backendsDir, ObjectMapper objectMapper) throws IOException {
        ExportBackendIndex index = new ExportBackendIndex();
        if (backendsDir == null || !Files.isDirectory(backendsDir)) {
            return index;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backendsDir, "*.json")) {
            for (Path file : stream) {
                Map<String, Object> backend = objectMapper.readValue(
                        Files.readString(file), new TypeReference<Map<String, Object>>() {});
                long id = toLong(backend.get("id"));
                String systemName = stringValue(backend.get("system_name"));
                String name = stringValue(backend.get("name"));
                String endpoint = stringValue(backend.get("private_endpoint"));
                BackendRecord record = new BackendRecord(id, systemName, name, endpoint);
                if (id > 0) {
                    index.byId.put(id, record);
                }
                if (!systemName.isBlank()) {
                    index.bySystemName.put(systemName, record);
                }
            }
        }
        return index;
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
