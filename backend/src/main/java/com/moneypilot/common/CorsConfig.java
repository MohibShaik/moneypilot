package com.moneypilot.common;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for the REST API.
 *
 * <p>This is a stateless, Bearer-token API (no cookies, no browser session), so
 * cross-origin access is safe to open for development: the Expo mobile client,
 * the Swagger UI, and local tooling all call the API from different origins.
 *
 * <p>Allowed origins are configurable via {@code MONEYPILOT_CORS_ALLOWED_ORIGINS}
 * (comma-separated). The default {@code *} allows any origin. Credentials are
 * NOT allowed (none are used), which keeps the wildcard origin valid under the
 * CORS spec. For a locked-down production deployment, set the env var to the
 * specific web origins that need browser access.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    public CorsConfig(
            @Value("${moneypilot.cors.allowed-origins:*}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Use origin patterns so a wildcard works uniformly; credentials stay
        // disabled, so "*" is permitted by the spec.
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
