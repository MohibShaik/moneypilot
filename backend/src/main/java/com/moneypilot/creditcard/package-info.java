/**
 * Credit card module: user credit cards and their statements.
 *
 * <p>Built on the V2 {@code credit_cards} and {@code card_statements} tables.
 * Ownership is enforced from the JWT subject; statements are reached only
 * through their owned parent card. This module also exposes
 * {@code CreditCardRepository.findByIdAndUserId}, which the Payment Methods
 * module uses to validate card ownership when linking a CREDIT_CARD method.
 */
package com.moneypilot.creditcard;
