package com.moneypilot.creditcard;

/**
 * Credit card status, matching the backend V2 CHECK constraint
 * {@code chk_credit_cards_status}.
 */
public enum CardStatus {
    ACTIVE,
    BLOCKED,
    CLOSED
}
