package com.moneypilot.creditcard;

/**
 * Raised when a credit card does not exist, or exists but is not owned by the
 * requesting user. Both map to {@code 404 CREDIT_CARD_NOT_FOUND} so the
 * response does not reveal whether another user's id exists.
 */
public class CreditCardNotFoundException extends RuntimeException {

    public CreditCardNotFoundException() {
        super("Credit card not found");
    }
}
