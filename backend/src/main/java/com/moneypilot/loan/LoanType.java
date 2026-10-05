package com.moneypilot.loan;

/**
 * Loan category, matching the backend V2 CHECK constraint {@code chk_loans_type}.
 */
public enum LoanType {
    HOME,
    CAR,
    PERSONAL,
    EDUCATION,
    GOLD,
    OTHER
}
