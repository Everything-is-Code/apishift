package io.apishift.util;

public final class LogSanitizer {

    private static final String UNSAFE_LOG_CHARS = "[\\r\\n]";

    private LogSanitizer() {}

    /** Strip CR/LF from user-controlled values before logging (log injection). */
    public static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll(UNSAFE_LOG_CHARS, "_");
    }
}
