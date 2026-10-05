package com.moneypilot.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public user profile returned by the API. Deliberately contains no
 * {@code passwordHash} field — the password hash is never exposed through any
 * response.
 */
public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String currency,
        String timezone,
        OffsetDateTime createdAt
) {
}
