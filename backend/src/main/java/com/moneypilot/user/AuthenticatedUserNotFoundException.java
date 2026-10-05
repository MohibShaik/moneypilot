package com.moneypilot.user;

/**
 * Raised when an authenticated principal's user id does not resolve to a user
 * (e.g. the user was deleted after the token was issued). Treated as an
 * authentication failure (HTTP 401) so the response never reveals whether a
 * given id exists.
 */
public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException() {
        super("Authenticated user not found");
    }
}
