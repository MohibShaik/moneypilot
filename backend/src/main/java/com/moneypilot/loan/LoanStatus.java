package com.moneypilot.loan;

/**
 * Loan lifecycle status, matching the backend V2 CHECK constraint
 * {@code chk_loans_status}.
 */
public enum LoanStatus {
    ACTIVE,
    CLOSED,
    FORECLOSED
}
