/**
 * Loan module: user loans and their generated EMI (installment) schedules.
 *
 * <p>Built on the V2 {@code loans} and {@code loan_emis} tables. Ownership is
 * enforced from the JWT subject; EMI schedules are generated server-side with a
 * reducing-balance amortization that satisfies the DB component-sum CHECK.
 */
package com.moneypilot.loan;
