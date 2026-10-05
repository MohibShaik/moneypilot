package com.moneypilot.paymentmethod;

/**
 * Payment method type, matching the backend V2 CHECK constraint
 * {@code chk_payment_methods_type}.
 */
public enum PaymentMethodType {
    CASH,
    UPI,
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    OTHER
}
