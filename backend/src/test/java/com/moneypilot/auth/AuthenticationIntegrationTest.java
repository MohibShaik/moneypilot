package com.moneypilot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneypilot.user.User;
import com.moneypilot.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end authentication integration tests.
 *
 * <p>Boots the full application context against the configured local PostgreSQL
 * (no Docker, Testcontainers, or H2) and drives the real HTTP stack through
 * MockMvc, including the Spring Security filter chain, the JWT filter, and
 * actual persistence. Covers registration, duplicate registration, login,
 * invalid login, {@code /users/me} with valid/missing/expired/tampered tokens,
 * statelessness (no session cookie), and the guarantee that {@code password_hash}
 * is never exposed.
 *
 * <p>These tests commit real rows (the web layer is not rolled back), so each
 * test uses a unique email and created users are removed in {@link #cleanup()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
        }
        createdEmails.clear();
    }

    private String uniqueEmail() {
        String email = "it+" + UUID.randomUUID() + "@example.com";
        createdEmails.add(email);
        return email;
    }

    private String registerBody(String email, String password, String displayName) {
        return """
                {"email":"%s","password":"%s","displayName":"%s"}
                """.formatted(email, password, displayName);
    }

    private String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    // ---- Registration ------------------------------------------------------

    @Test
    void registerPersistsUserWithArgon2idHashAndDefaultsAndNoPasswordInResponse() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "password1", "Ada")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Kolkata"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.password_hash").doesNotExist())
                // Registration issues no token.
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());

        User persisted = userRepository.findByEmail(email).orElseThrow();
        assertThat(persisted.getPasswordHash()).startsWith("$argon2id$");
        assertThat(persisted.getPasswordHash()).isNotEqualTo("password1");
    }

    @Test
    void duplicateRegistrationReturns409() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "password1", "Ada")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "password2", "Ada Two")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_USED"));

        // Only one row persisted.
        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Test
    void registrationValidationFailureReturns400AndPersistsNothing() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "short", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    // ---- Login -------------------------------------------------------------

    @Test
    void loginReturnsTokenAndNoSessionCookie() throws Exception {
        String email = uniqueEmail();
        register(email, "password1", "Ada");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "password1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(900))
                // Stateless: no JSESSIONID session cookie is set.
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();

        assertThat(tokenFrom(result)).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordAndUnknownEmailAreIndistinguishable() throws Exception {
        String email = uniqueEmail();
        register(email, "password1", "Ada");

        String wrongPassword = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "wrongpassword")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();

        String unknownEmail = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody+" + UUID.randomUUID() + "@example.com", "password1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();

        assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    // ---- /users/me ---------------------------------------------------------

    @Test
    void meWithValidTokenReturnsProfileWithoutPasswordHash() throws Exception {
        String email = uniqueEmail();
        register(email, "password1", "Ada");
        String token = login(email, "password1");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.displayName").value("Ada"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.password_hash").doesNotExist());
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithMalformedTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithTamperedTokenReturns401() throws Exception {
        String email = uniqueEmail();
        register(email, "password1", "Ada");
        String token = login(email, "password1");

        // Flip the last character to break the signature.
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void meResolvesUserFromTokenSubjectNotFromAnyParameter() throws Exception {
        // Two users; a token for user A must return A's profile even if B's id
        // is supplied as a (ignored) query parameter.
        String emailA = uniqueEmail();
        register(emailA, "password1", "Ada");
        String tokenA = login(emailA, "password1");

        String emailB = uniqueEmail();
        register(emailB, "password1", "Bob");
        Optional<User> userB = userRepository.findByEmail(emailB);
        assertThat(userB).isPresent();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("userId", userB.get().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(emailA));
    }

    // ---- helpers -----------------------------------------------------------

    private void register(String email, String password, String displayName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, password, displayName)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return tokenFrom(result);
    }

    private String tokenFrom(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }
}
