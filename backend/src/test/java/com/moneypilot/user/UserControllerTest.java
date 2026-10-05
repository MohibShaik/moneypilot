package com.moneypilot.user;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moneypilot.auth.security.JwtAuthenticationFilter;
import com.moneypilot.auth.security.JwtService;
import com.moneypilot.auth.security.RestAuthenticationEntryPoint;
import com.moneypilot.common.GlobalExceptionHandler;
import com.moneypilot.common.SecurityConfig;
import com.moneypilot.user.dto.UserResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Web-slice test for {@link UserController}. The service is mocked and the
 * authenticated principal is supplied via Spring Security's test support, so
 * this verifies that the endpoint resolves the user id from the security
 * principal (not a request parameter) and returns the public profile without a
 * password hash.
 *
 * <p>MockMvc is built with {@code springSecurity()} so the test-provided
 * authentication is propagated into the request's security context and injected
 * into the controller's {@link org.springframework.security.core.Authentication}
 * parameter.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class,
        JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void meReturnsAuthenticatedUserProfile() throws Exception {
        UUID userId = UUID.fromString("7f000000-0000-0000-0000-0000000000e2");
        UserResponse profile = new UserResponse(
                userId, "ada@example.com", "Ada", "INR", "Asia/Kolkata",
                OffsetDateTime.of(2026, 10, 3, 10, 0, 0, 0, ZoneOffset.UTC));
        when(userService.getCurrentUser(eq(userId))).thenReturn(profile);

        var principal = new UsernamePasswordAuthenticationToken(
                userId.toString(), null, AuthorityUtils.NO_AUTHORITIES);

        mockMvc.perform(get("/api/v1/users/me").with(authentication(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("ada@example.com"))
                .andExpect(jsonPath("$.data.displayName").value("Ada"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.password_hash").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}
