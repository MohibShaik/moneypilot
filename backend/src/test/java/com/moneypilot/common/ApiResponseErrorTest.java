package com.moneypilot.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiResponseErrorTest {

    @Test
    void errorFactoryProducesFailureEnvelope() {
        ApiError error = ApiError.of("VALIDATION_ERROR", "Validation failed");

        ApiResponse<Object> response = ApiResponse.error(error);

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isSameAs(error);
    }

    @Test
    void okFactoryStillProducesSuccessEnvelope() {
        ApiResponse<String> response = ApiResponse.ok("payload");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.error()).isNull();
    }

    @Test
    void apiErrorWithoutFieldErrorsHasNullMap() {
        ApiError error = ApiError.of("BAD_REQUEST", "Malformed request body");

        assertThat(error.code()).isEqualTo("BAD_REQUEST");
        assertThat(error.message()).isEqualTo("Malformed request body");
        assertThat(error.fieldErrors()).isNull();
    }

    @Test
    void apiErrorCarriesFieldErrors() {
        Map<String, String> fields = Map.of("password", "size must be between 8 and 128");
        ApiError error = ApiError.of("VALIDATION_ERROR", "Validation failed", fields);

        assertThat(error.fieldErrors()).containsEntry("password", "size must be between 8 and 128");
    }
}
