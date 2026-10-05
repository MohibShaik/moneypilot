package com.moneypilot.auth.dto;

import com.moneypilot.common.validation.ValidZoneId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration request payload. Input only; {@code password} is never echoed
 * back in any response.
 *
 * <p>{@code currency} and {@code timezone} are optional: when omitted the
 * service applies the defaults ({@code INR} / {@code Asia/Kolkata}). When
 * supplied they must satisfy the validation constraints below.
 */
public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 128)
        String password,

        @NotBlank
        @Size(min = 1, max = 100)
        String displayName,

        @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter uppercase ISO code")
        String currency,

        @ValidZoneId
        String timezone
) {
}
