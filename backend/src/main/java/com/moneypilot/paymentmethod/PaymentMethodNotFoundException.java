package com.moneypilot.paymentmethod;

/**
 * Raised when a payment method does not exist, or exists but is not owned by
 * the requesting user. Both map to {@code 404 PAYMENT_METHOD_NOT_FOUND} so the
 * response does not reveal whether another user's id exists.
 */
public class PaymentMethodNotFoundException extends RuntimeException {

    public PaymentMethodNotFoundException() {
        super("Payment method not found");
    }
}
