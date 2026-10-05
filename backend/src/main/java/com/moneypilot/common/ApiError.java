package com.moneypilot.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Structured error payload carried by a failed {@link ApiResponse}.
 *
 * <p>{@code fieldErrors} is only present for validation failures and is omitted
 * from the serialized JSON when {@code null}.
 *
 * @param code        a stable, machine-readable error code (e.g. {@code VALIDATION_ERROR})
 * @param message     a human-readable, generic message safe to expose to clients
 * @param fieldErrors optional map of field name to validation message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, Map<String, String> fieldErrors) {

    /**
     * Builds an error without field-level details.
     */
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    /**
     * Builds an error with field-level validation details.
     */
    public static ApiError of(String code, String message, Map<String, String> fieldErrors) {
        return new ApiError(code, message, fieldErrors);
    }
}
