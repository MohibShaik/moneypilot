package com.moneypilot.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneypilot.common.ApiError;
import com.moneypilot.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Returns a consistent {@code 401 UNAUTHORIZED} {@link ApiResponse} error for
 * any request that reaches a protected endpoint without valid authentication
 * (missing, malformed, expired, or invalid token). The message is generic and
 * identical regardless of the underlying cause.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Object> body =
                ApiResponse.error(ApiError.of("UNAUTHORIZED", "Authentication required"));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
