package com.moneypilot.payment;

/**
 * Raised when a payment does not exist, or exists but is not owned by the
 * requesting user. Both map to {@code 404 PAYMENT_NOT_FOUND} so the response
 * does not reveal whether another user's id exists.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException() {
        super("Payment not found");
    }
}
