package com.moneypilot.auth;

/**
 * Raised during registration when the email is already in use.
 *
 * <p>This is thrown either by the {@code existsByEmail} fast-path check or by
 * translating a database unique-constraint violation on {@code uq_users_email}.
 * It is deliberately generic: the global error handler maps it to a
 * {@code 409 EMAIL_ALREADY_USED} response without exposing the constraint name.
 */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Email already used");
    }
}
