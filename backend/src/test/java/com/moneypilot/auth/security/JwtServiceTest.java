package com.moneypilot.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";
    private static final String ISSUER = "moneypilot";

    private JwtService serviceWith(Duration ttl, String issuer) {
        return new JwtService(new JwtProperties(SECRET, ttl, issuer));
    }

    private JwtService service() {
        return serviceWith(Duration.ofMinutes(15), ISSUER);
    }

    @Test
    void issuesTokenAndRoundTripsSubject() {
        JwtService service = service();
        UUID userId = UUID.randomUUID();

        String token = service.issueToken(userId);
        assertThat(token).isNotBlank();
        assertThat(service.validateAndGetUserId(token)).isEqualTo(userId);
    }

    @Test
    void ttlSecondsReflectsConfiguration() {
        assertThat(service().getTtlSeconds()).isEqualTo(900);
    }

    @Test
    void tokenContainsOnlyMinimalClaims() {
        JwtService service = service();
        UUID userId = UUID.randomUUID();
        String token = service.issueToken(userId);

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        // No extra / sensitive claims.
        assertThat(claims.keySet())
                .containsExactlyInAnyOrder("sub", "iat", "exp", "iss");
        assertThat(claims.keySet()).doesNotContain("password", "passwordHash", "email");
    }

    @Test
    void omitsIssuerClaimWhenNotConfigured() {
        JwtService service = serviceWith(Duration.ofMinutes(15), "");
        String token = service.issueToken(UUID.randomUUID());

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getIssuer()).isNull();
        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "iat", "exp");
    }

    @Test
    void rejectsExpiredToken() {
        // Negative TTL -> expiration already in the past.
        JwtService service = serviceWith(Duration.ofMinutes(-1), ISSUER);
        String token = service.issueToken(UUID.randomUUID());

        JwtService validator = service();
        assertThatThrownBy(() -> validator.validateAndGetUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedSignature() {
        JwtService service = service();
        String token = service.issueToken(UUID.randomUUID());

        // Flip the last character of the signature segment.
        char last = token.charAt(token.length() - 1);
        char replacement = (last == 'A') ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        assertThatThrownBy(() -> service.validateAndGetUserId(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtService service = service();
        String foreign = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(
                        "another-secret-another-secret-0123456789".getBytes(StandardCharsets.UTF_8)),
                        Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> service.validateAndGetUserId(foreign))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenWithWrongAlgorithm() {
        // Sign with HS512 (a different algorithm) using a key long enough for it;
        // the HS256-configured service must reject it.
        SecretKey hs512Key = Keys.hmacShaKeyFor(
                ("test-secret-test-secret-test-secret-test-secret-test-secret-0123456789")
                        .getBytes(StandardCharsets.UTF_8));
        String hs512Token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(hs512Key, Jwts.SIG.HS512)
                .compact();

        assertThatThrownBy(() -> service().validateAndGetUserId(hs512Token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenWithWrongIssuer() {
        JwtService issuerA = serviceWith(Duration.ofMinutes(15), "someone-else");
        String token = issuerA.issueToken(UUID.randomUUID());

        JwtService validator = serviceWith(Duration.ofMinutes(15), ISSUER);
        assertThatThrownBy(() -> validator.validateAndGetUserId(token))
                .isInstanceOf(JwtException.class);
    }
}
