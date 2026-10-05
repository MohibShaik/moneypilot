package com.moneypilot.common;

import com.moneypilot.auth.security.JwtAuthenticationFilter;
import com.moneypilot.auth.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security configuration.
 *
 * <p>Per the security architecture and auth LLD:
 * <ul>
 *   <li>Session management is {@code STATELESS}; CSRF, form login, and HTTP
 *       Basic are disabled (this is a token-based REST API).</li>
 *   <li>Public endpoints: register, login, health, and the OpenAPI/Swagger
 *       documentation ({@code /v3/api-docs}, {@code /swagger-ui}).</li>
 *   <li>Every other endpoint requires authentication.</li>
 *   <li>The {@link JwtAuthenticationFilter} runs before the username/password
 *       filter and establishes authentication from a Bearer token.</li>
 *   <li>Unauthenticated access to a protected endpoint yields a consistent
 *       {@code 401} via {@link RestAuthenticationEntryPoint}.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Apply the CorsConfigurationSource bean (see CorsConfig).
            .cors(cors -> {})
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // OpenAPI spec + Swagger UI (API documentation, public).
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex ->
                    ex.authenticationEntryPoint(authenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
