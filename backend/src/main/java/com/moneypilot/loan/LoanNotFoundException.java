package com.moneypilot.loan;

/**
 * Raised when a loan does not exist, or exists but is not owned by the
 * requesting user. Both map to {@code 404 LOAN_NOT_FOUND} so the response does
 * not reveal whether another user's id exists.
 */
public class LoanNotFoundException extends RuntimeException {

    public LoanNotFoundException() {
        super("Loan not found");
    }
}
