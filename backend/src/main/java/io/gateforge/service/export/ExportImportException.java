package io.gateforge.service.export;

public class ExportImportException extends RuntimeException {

    public ExportImportException(String message) {
        super(message);
    }

    public ExportImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
