package com.moneypilot.common;

/**
 * Standard envelope for REST responses: a success flag, a data payload, and an
 * optional error. Kept intentionally simple for the initial foundation.
 *
 * @param <T> the type of the data payload
 */
public record ApiResponse<T>(boolean success, T data, Object error) {

    /**
     * Builds a successful response wrapping the given payload with no error.
     *
     * @param data the payload to return
     * @param <T>  the payload type
     * @return a success response
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * Builds a failed response carrying the given structured error and no data.
     *
     * @param error the error payload
     * @param <T>   the (absent) payload type
     * @return a failure response with {@code success=false} and {@code data=null}
     */
    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
