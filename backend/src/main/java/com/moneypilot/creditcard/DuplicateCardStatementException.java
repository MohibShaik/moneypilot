package com.moneypilot.creditcard;

/**
 * Raised when a statement with the same statement date already exists for a
 * card (DB {@code uq_card_statements_card_date}). Maps to
 * {@code 409 DUPLICATE_CARD_STATEMENT}.
 */
public class DuplicateCardStatementException extends RuntimeException {

    public DuplicateCardStatementException() {
        super("A statement with this date already exists for the card");
    }
}
