package com.finshield.backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        String requestId,
        Instant timestamp,
        List<ValidationError> validationErrors
) {

    public ErrorResponse {
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("status must be a valid HTTP error status");
        }
        error = requireText(error, "error");
        message = requireText(message, "message");
        path = requireText(path, "path");
        requestId = requireText(requestId, "requestId");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
    }

    public static ErrorResponse of(
            int status,
            String error,
            String message,
            String path,
            String requestId
    ) {
        return new ErrorResponse(status, error, message, path, requestId, Instant.now(), List.of());
    }

    public static ErrorResponse withValidationErrors(
            int status,
            String error,
            String message,
            String path,
            String requestId,
            List<ValidationError> validationErrors
    ) {
        return new ErrorResponse(
                status, error, message, path, requestId, Instant.now(), validationErrors
        );
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public record ValidationError(String field, String message) {
        public ValidationError {
            field = requireText(field, "field");
            message = requireText(message, "message");
        }
    }
}
