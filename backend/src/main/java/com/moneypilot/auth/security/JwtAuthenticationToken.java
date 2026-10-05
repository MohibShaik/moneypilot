package com.moneypilot.auth.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authentication established from a validated JWT. The principal is the user's
 * {@link UUID} (the token subject). There are no granted authorities beyond the
 * implicit "authenticated" state; the single-role model is intentional for the
 * current scope.
 *
 * <p>{@link #getName()} returns the user id string so controllers can resolve
 * the authenticated user from the security context without any client-supplied
 * identifier.
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID userId;

    public JwtAuthenticationToken(UUID userId) {
        super(List.of());
        this.userId = userId;
        setAuthenticated(true);
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public Object getCredentials() {
        // Credentials are not retained after authentication.
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
