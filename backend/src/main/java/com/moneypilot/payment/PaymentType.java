package com.moneypilot.payment;

/**
 * Payment target type, matching the backend V2 CHECK constraints
 * {@code chk_payments_type} and {@code chk_payments_target}:
 * <ul>
 *   <li>{@code LOAN_EMI} — targets a loan EMI ({@code loan_emi_id} set).</li>
 *   <li>{@code CREDIT_CARD} — targets a card statement ({@code card_statement_id} set).</li>
 *   <li>{@code OTHER} — untargeted (both target ids null).</li>
 * </ul>
 */
public enum PaymentType {
    LOAN_EMI,
    CREDIT_CARD,
    OTHER
}
