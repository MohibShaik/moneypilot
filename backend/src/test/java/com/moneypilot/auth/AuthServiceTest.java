package com.moneypilot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneypilot.auth.dto.LoginRequest;
import com.moneypilot.auth.dto.RegisterRequest;
import com.moneypilot.auth.dto.TokenResponse;
import com.moneypilot.auth.security.JwtService;
import com.moneypilot.user.User;
import com.moneypilot.user.UserMapper;
import com.moneypilot.user.UserRepository;
import com.moneypilot.user.dto.UserResponse;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        jwtService = org.mockito.Mockito.mock(JwtService.class);
        UserMapper userMapper = new UserMapper();
        authService = new AuthService(userRepository, userMapper, passwordEncoder, jwtService);
    }

    private RegisterRequest request(String currency, String timezone) {
        return new RegisterRequest("ada@example.com", "password1", "Ada", currency, timezone);
    }

    private LoginRequest loginRequest() {
        return new LoginRequest("ada@example.com", "password1");
    }

    @Test
    void registerEncodesPasswordAndReturnsProfileWithoutToken() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("$argon2id$ENCODED");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(request("USD", "America/New_York"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User persisted = captor.getValue();

        assertThat(persisted.getPasswordHash()).isEqualTo("$argon2id$ENCODED");
        assertThat(persisted.getEmail()).isEqualTo("ada@example.com");
        assertThat(persisted.getCurrency()).isEqualTo("USD");
        assertThat(persisted.getTimezone()).isEqualTo("America/New_York");

        // Returned profile reflects the saved entity and carries no password data.
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.toString()).doesNotContain("$argon2id$ENCODED");
    }

    @Test
    void registerAppliesDefaultsWhenCurrencyAndTimezoneOmitted() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$argon2id$ENCODED");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(request(null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo("INR");
        assertThat(captor.getValue().getTimezone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void registerThrowsOnFastPathDuplicateEmail() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request(null, null)))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registerTranslatesEmailUniqueViolationToEmailAlreadyUsed() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$argon2id$ENCODED");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(dataIntegrityViolation("uq_users_email"));

        assertThatThrownBy(() -> authService.register(request(null, null)))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void registerDoesNotMisclassifyOtherIntegrityViolations() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$argon2id$ENCODED");
        DataIntegrityViolationException other = dataIntegrityViolation("some_other_constraint");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(other);

        assertThatThrownBy(() -> authService.register(request(null, null)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void registerPropagatesIntegrityViolationWithNoHibernateCause() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$argon2id$ENCODED");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("opaque"));

        assertThatThrownBy(() -> authService.register(request(null, null)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void authenticateReturnsTokenForValidCredentials() {
        UUID userId = UUID.randomUUID();
        User user = new User("ada@example.com", "Ada", "$argon2id$STORED", "INR", "Asia/Kolkata");
        user.setId(userId);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "$argon2id$STORED")).thenReturn(true);
        when(jwtService.issueToken(userId)).thenReturn("jwt-token");
        when(jwtService.getTtlSeconds()).thenReturn(900L);

        TokenResponse response = authService.authenticate(loginRequest());

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
    }

    @Test
    void authenticateThrowsInvalidCredentialsForUnknownEmail() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(loginRequest()))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).issueToken(any());
    }

    @Test
    void authenticateThrowsInvalidCredentialsForWrongPassword() {
        User user = new User("ada@example.com", "Ada", "$argon2id$STORED", "INR", "Asia/Kolkata");
        user.setId(UUID.randomUUID());
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "$argon2id$STORED")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(loginRequest()))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).issueToken(any());
    }

    @Test
    void authenticateFailuresAreIndistinguishable() {
        // Unknown email and wrong password throw the same exception type/message.
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        Throwable unknownEmail = org.assertj.core.api.Assertions.catchThrowable(
                () -> authService.authenticate(loginRequest()));

        User user = new User("ada@example.com", "Ada", "$argon2id$STORED", "INR", "Asia/Kolkata");
        user.setId(UUID.randomUUID());
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        Throwable wrongPassword = org.assertj.core.api.Assertions.catchThrowable(
                () -> authService.authenticate(loginRequest()));

        assertThat(unknownEmail).isInstanceOf(InvalidCredentialsException.class);
        assertThat(wrongPassword).isInstanceOf(InvalidCredentialsException.class);
        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
    }

    private static DataIntegrityViolationException dataIntegrityViolation(String constraintName) {
        ConstraintViolationException cve = new ConstraintViolationException(
                "constraint violation",
                new SQLException("duplicate key", "23505"),
                constraintName);
        return new DataIntegrityViolationException("wrapper", cve);
    }
}
