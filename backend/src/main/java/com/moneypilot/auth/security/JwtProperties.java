package com.moneypilot.auth.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized JWT configuration bound from the {@code moneypilot.jwt.*} keys.
 *
 * <p>The secret must be supplied via the environment (never committed) and must
 * be long enough for HS256 (a 256-bit / 32-byte key). Validation here makes the
 * application fail fast at startup when the secret is missing or too short.
 *
 * @param secret the HMAC-SHA256 signing secret (>= 32 characters)
 * @param ttl    access-token lifetime; defaults to 15 minutes
 * @param issuer optional issuer ({@code iss}) claim; may be blank to omit
 */
@Validated
@ConfigurationProperties(prefix = "moneypilot.jwt")
public record JwtProperties(

        @NotBlank(message = "JWT secret must be provided (env MONEYPILOT_JWT_SECRET)")
        @Size(min = 32, message = "JWT secret must be at least 32 characters for HS256")
        String secret,

        @NotNull
        Duration ttl,

        String issuer
) {

    public JwtProperties {
        if (ttl == null) {
            ttl = Duration.ofMinutes(15);
        }
    }

    /** Whether an issuer is configured (non-blank). */
    public boolean hasIssuer() {
        return issuer != null && !issuer.isBlank();
    }
}
