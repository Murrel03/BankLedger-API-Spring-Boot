package com.murrel.bankledger.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Consistent JSON shape for every error this API returns. */
public record ErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, null, LocalDateTime.now());
    }

    public static ErrorResponse validation(String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(400, "Bad Request", message, fieldErrors, LocalDateTime.now());
    }
}
