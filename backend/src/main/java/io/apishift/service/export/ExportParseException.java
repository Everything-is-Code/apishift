package io.apishift.service.export;

public class ExportParseException extends RuntimeException {

    public ExportParseException(String message) {
        super(message);
    }

    public ExportParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
