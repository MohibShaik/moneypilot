/**
 * Payment module: user payments against loan EMIs, credit-card statements, or
 * untargeted "other" payments.
 *
 * <p>Built on the V2 {@code payments} table. Ownership is enforced from the JWT
 * subject; the type/target shape and target ownership are validated before
 * persisting, and recording or deleting a payment settles the targeted loan EMI
 * or card statement within the same transaction.
 */
package com.moneypilot.payment;
