package com.moneypilot.creditcard;

/**
 * Raised when a card statement does not exist, or exists but does not belong to
 * an owned card. Maps to {@code 404 CARD_STATEMENT_NOT_FOUND}.
 */
public class CardStatementNotFoundException extends RuntimeException {

    public CardStatementNotFoundException() {
        super("Card statement not found");
    }
}
