package com.moneypilot.auth;

import com.moneypilot.auth.dto.LoginRequest;
import com.moneypilot.auth.dto.RegisterRequest;
import com.moneypilot.auth.dto.TokenResponse;
import com.moneypilot.common.ApiResponse;
import com.moneypilot.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 *
 * <p>Thin by design: it validates the request with {@code @Valid}, delegates to
 * {@link AuthService}, and wraps the result in the standard {@link ApiResponse}
 * envelope. All business logic lives in the service; error translation lives in
 * the global exception handler.
 *
 * <p>Registration returns the public user profile with HTTP 201 and does NOT
 * issue a JWT. Login verifies credentials and returns an access token.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register and log in. These endpoints are public.")
@SecurityRequirements // public: no bearer token required
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user",
            description = "Creates a user and returns the public profile (201). Does not issue a token.")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in",
            description = "Verifies credentials and returns a JWT access token.")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.authenticate(request));
    }
}
