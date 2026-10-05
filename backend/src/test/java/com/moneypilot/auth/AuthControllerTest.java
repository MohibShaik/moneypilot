package com.moneypilot.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moneypilot.auth.dto.RegisterRequest;
import com.moneypilot.auth.dto.TokenResponse;
import com.moneypilot.auth.security.JwtAuthenticationFilter;
import com.moneypilot.auth.security.JwtService;
import com.moneypilot.auth.security.RestAuthenticationEntryPoint;
import com.moneypilot.common.GlobalExceptionHandler;
import com.moneypilot.common.SecurityConfig;
import com.moneypilot.user.dto.UserResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice test for {@link AuthController} registration. The service is mocked,
 * so no database is required; this verifies HTTP status, the ApiResponse
 * envelope, validation behavior, and error mapping via the global handler.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class,
        JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    private UserResponse sampleProfile() {
        return new UserResponse(
                UUID.fromString("7f000000-0000-0000-0000-0000000000e2"),
                "ada@example.com",
                "Ada",
                "INR",
                "Asia/Kolkata",
                OffsetDateTime.of(2026, 10, 3, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void registerReturns201WithProfileEnvelopeAndNoSensitiveData() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleProfile());

        String body = """
                {"email":"ada@example.com","password":"password1","displayName":"Ada"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("ada@example.com"))
                .andExpect(jsonPath("$.data.displayName").value("Ada"))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Kolkata"))
                .andExpect(jsonPath("$.error").doesNotExist())
                // No token on registration and no password hash ever.
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.password_hash").doesNotExist());
    }

    @Test
    void registerReturns400ForInvalidRequestAndDoesNotCallService() throws Exception {
        // Blank display name, short password, malformed email.
        String body = """
                {"email":"not-an-email","password":"short","displayName":""}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void registerReturns409WhenEmailAlreadyUsed() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyUsedException());

        String body = """
                {"email":"ada@example.com","password":"password1","displayName":"Ada"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_USED"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void registerReturns400ForMalformedJsonBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void loginReturns200WithTokenEnvelope() throws Exception {
        when(authService.authenticate(any()))
                .thenReturn(new TokenResponse("jwt-token", "Bearer", 900L));

        String body = """
                {"email":"ada@example.com","password":"password1"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void loginReturns401ForInvalidCredentials() throws Exception {
        when(authService.authenticate(any()))
                .thenThrow(new InvalidCredentialsException());

        String body = """
                {"email":"ada@example.com","password":"wrongpassword"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void loginReturns400ForMissingFields() throws Exception {
        String body = """
                {"email":"","password":""}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verify(authService, never()).authenticate(any());
    }
}
