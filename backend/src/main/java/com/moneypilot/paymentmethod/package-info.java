/**
 * Payment methods module: a user's ways to pay (cash, UPI, debit card, bank
 * transfer, credit card, other).
 *
 * <p>Ownership is enforced from the JWT subject against
 * {@code payment_methods.user_id}; a user may only read and manage their own
 * payment methods. In V1 the {@code CREDIT_CARD} type is a temporary capability
 * boundary — it is rejected until the Credit Cards module exists to validate
 * credit-card ownership — because the database FK/CHECK can enforce the link's
 * shape but not ownership.
 */
package com.moneypilot.paymentmethod;
