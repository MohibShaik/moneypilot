package com.moneypilot.auth;

import com.moneypilot.auth.dto.LoginRequest;
import com.moneypilot.auth.dto.RegisterRequest;
import com.moneypilot.auth.dto.TokenResponse;
import com.moneypilot.auth.security.JwtService;
import com.moneypilot.user.User;
import com.moneypilot.user.UserMapper;
import com.moneypilot.user.UserRepository;
import com.moneypilot.user.dto.UserResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication/registration business logic.
 *
 * <p>Registration hashes the password with Argon2id, assigns currency/timezone
 * defaults when omitted, persists the user, and returns the public profile. Per
 * the auth LLD, registration does NOT issue a JWT.
 *
 * <p>Login verifies the password against the stored Argon2id hash and, on
 * success, issues an HS256 JWT. A failed login — whether the email is unknown or
 * the password is wrong — always yields the same generic
 * {@link InvalidCredentialsException}, so the response never reveals which
 * condition occurred.
 *
 * <p>Email uniqueness: a fast {@code existsByEmail} pre-check is an optimization,
 * but the database unique constraint {@code uq_users_email} is the source of
 * truth. Only a violation of that specific constraint is translated to
 * {@link EmailAlreadyUsedException}; any other integrity violation propagates
 * unchanged so it is not misclassified as a duplicate-email error.
 */
@Service
public class AuthService {

    /** Name of the unique constraint on {@code users.email} in the V2 schema. */
    static final String EMAIL_UNIQUE_CONSTRAINT = "uq_users_email";

    static final String DEFAULT_CURRENCY = "INR";
    static final String DEFAULT_TIMEZONE = "Asia/Kolkata";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, UserMapper userMapper,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user and returns the public profile. No token is issued.
     *
     * @throws EmailAlreadyUsedException if the email is already registered
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyUsedException();
        }

        User user = new User(
                request.email(),
                request.displayName(),
                passwordEncoder.encode(request.password()),
                request.currency() != null ? request.currency() : DEFAULT_CURRENCY,
                request.timezone() != null ? request.timezone() : DEFAULT_TIMEZONE);

        User saved;
        try {
            // saveAndFlush forces the INSERT now so the database-generated id and
            // created_at/updated_at are populated, and any unique-constraint
            // violation surfaces here where it can be translated precisely.
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            if (isEmailUniqueViolation(ex)) {
                throw new EmailAlreadyUsedException();
            }
            // Different integrity violation: do not misclassify as duplicate email.
            throw ex;
        }

        return userMapper.toResponse(saved);
    }

    /**
     * Authenticates a user and, on success, issues an HS256 access token.
     *
     * <p>An unknown email and a wrong password both throw the same
     * {@link InvalidCredentialsException} so the outcome is indistinguishable.
     *
     * @throws InvalidCredentialsException if the credentials are invalid
     */
    @Transactional(readOnly = true)
    public TokenResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.issueToken(user.getId());
        return new TokenResponse(token, TokenResponse.BEARER, jwtService.getTtlSeconds());
    }

    /**
     * Returns true only when the violation is on the {@code uq_users_email}
     * constraint. Unwraps the Hibernate {@link ConstraintViolationException} to
     * read the constraint name; never relies on a bare SQLState.
     */
    private boolean isEmailUniqueViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                String name = cve.getConstraintName();
                return name != null && name.toLowerCase().contains(EMAIL_UNIQUE_CONSTRAINT);
            }
            cause = cause.getCause();
        }
        return false;
    }
}
