package com.finova.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error payload returned for every failed request. A single shape makes
 * client-side error handling predictable and keeps logs/audits consistent.
 *
 * @param timestamp when the error was produced (UTC)
 * @param status    HTTP status code
 * @param error     short status reason phrase
 * @param message   human-readable summary
 * @param path      request URI that failed
 * @param details   optional field-level validation messages
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
    public static ApiError of(int status, String error, String message, String path, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, path, details == null ? List.of() : details);
    }
}
