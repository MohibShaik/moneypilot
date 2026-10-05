package com.moneypilot.user;

import com.moneypilot.common.ApiResponse;
import com.moneypilot.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile endpoints.
 *
 * <p>{@code GET /api/v1/users/me} returns the authenticated user's own profile.
 * The user id is taken exclusively from the security principal (the JWT
 * subject); there is no user-id request parameter, so a client cannot request
 * another user's profile by changing an identifier.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "The authenticated user's own profile.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my profile",
            description = "Returns the authenticated user's profile (from the JWT subject).")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ApiResponse.ok(userService.getCurrentUser(userId));
    }
}
