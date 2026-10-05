package com.moneypilot.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests that carry a {@code Authorization: Bearer <jwt>} header.
 *
 * <p>Behavior (auth LLD section 15):
 * <ul>
 *   <li>No {@code Authorization} header → continue unauthenticated.</li>
 *   <li>Header not using the {@code Bearer} scheme → continue unauthenticated.</li>
 *   <li>Valid Bearer token → set a {@link JwtAuthenticationToken} (principal =
 *       user UUID from the token subject) in the security context.</li>
 *   <li>Invalid/expired/tampered token → clear the context and continue; the
 *       entry point produces a 401 when a protected endpoint is reached.</li>
 * </ul>
 *
 * <p>The filter never writes a response itself; authorization decisions (and the
 * 401 for protected endpoints) are left to the security filter chain and the
 * {@link RestAuthenticationEntryPoint}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                UUID userId = jwtService.validateAndGetUserId(token);
                SecurityContextHolder.getContext()
                        .setAuthentication(new JwtAuthenticationToken(userId));
            } catch (Exception ex) {
                // Invalid token: ensure no stale authentication remains. Do not
                // write a response here — a protected endpoint will trigger the
                // entry point's 401.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
