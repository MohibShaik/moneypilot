package com.moneypilot.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request payload. Input only; {@code password} is never echoed back.
 *
 * <p>Only presence/shape is validated here; credential correctness is checked by
 * the authentication logic (implemented in a later task) and always yields a
 * single generic error.
 */
public record LoginRequest(

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 128)
        String password
) {
}
