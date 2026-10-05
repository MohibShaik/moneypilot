package com.moneypilot.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and validates HS256 JSON Web Tokens for authentication.
 *
 * <p>Tokens carry only the minimum claims required: {@code sub} (the user's
 * UUID), {@code iat}, {@code exp}, and {@code iss} when an issuer is configured.
 * No password or other sensitive data is included.
 *
 * <p>Validation verifies the signature, enforces expiration, and — because the
 * parser is built with a MAC verification key — only accepts HS256-signed
 * tokens; a token using any other algorithm is rejected. When an issuer is
 * configured it is also required to match.
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issues a signed HS256 token whose subject is the given user id.
     *
     * @param userId the authenticated user's UUID
     * @return the compact serialized JWT
     */
    public String issueToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.ttl());

        var builder = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        if (properties.hasIssuer()) {
            builder.issuer(properties.issuer());
        }

        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    /**
     * Validates the token (signature, algorithm, expiration, and issuer when
     * configured) and returns the subject as a {@link UUID}.
     *
     * @throws JwtException             if the token is invalid, expired, tampered,
     *                                  or uses a non-HS256 algorithm
     * @throws IllegalArgumentException if the subject is not a valid UUID
     */
    public UUID validateAndGetUserId(String token) {
        var parserBuilder = Jwts.parser().verifyWith(key);
        if (properties.hasIssuer()) {
            parserBuilder.requireIssuer(properties.issuer());
        }
        Claims claims = parserBuilder.build().parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }

    /** The configured access-token lifetime in seconds (for TokenResponse). */
    public long getTtlSeconds() {
        return properties.ttl().toSeconds();
    }
}
