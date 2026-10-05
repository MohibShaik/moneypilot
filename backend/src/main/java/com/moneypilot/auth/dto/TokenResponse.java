package com.moneypilot.auth.dto;

/**
 * Login response payload carrying the issued access token.
 *
 * <p>Token issuance is implemented in a later task; this type defines the
 * response shape per the approved design.
 *
 * @param accessToken      the signed JWT access token
 * @param tokenType        the token scheme, always {@code "Bearer"}
 * @param expiresInSeconds the access-token lifetime in seconds
 */
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {

    public static final String BEARER = "Bearer";
}
