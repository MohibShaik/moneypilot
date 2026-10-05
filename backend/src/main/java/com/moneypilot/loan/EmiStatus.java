package com.moneypilot.loan;

/**
 * EMI (installment) status, matching the backend V2 CHECK constraint
 * {@code chk_loan_emis_status}. New schedule rows start as {@code UPCOMING}.
 */
public enum EmiStatus {
    UPCOMING,
    DUE,
    PAID,
    OVERDUE,
    WAIVED
}
