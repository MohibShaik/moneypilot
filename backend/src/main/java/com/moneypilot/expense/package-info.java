/**
 * Expense module: user expenses with optional category and payment-method
 * references.
 *
 * <p>Built on the V2 {@code expenses} table. Ownership is enforced from the JWT
 * subject; a referenced category must be visible to the user (system or owned)
 * and a referenced payment method must be owned, validated before persisting.
 */
package com.moneypilot.expense;
