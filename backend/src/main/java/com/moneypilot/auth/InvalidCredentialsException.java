package com.moneypilot.auth;

/**
 * Raised on a failed login. Deliberately generic: it is thrown for both an
 * unknown email and a wrong password, so the API response never reveals which
 * condition occurred. The global error handler maps it to a
 * {@code 401 INVALID_CREDENTIALS} response.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
