package com.moneypilot.creditcard;

/**
 * Credit card statement status, matching the backend V2 CHECK constraint
 * {@code chk_card_statements_status}. New statements default to
 * {@code GENERATED}; the service derives PARTIALLY_PAID / PAID from the paid
 * amount. {@code OVERDUE} is time-based and set explicitly.
 */
public enum StatementStatus {
    GENERATED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE
}
