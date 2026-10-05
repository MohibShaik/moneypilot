package com.moneypilot.expense;

/**
 * Raised when an expense does not exist, or exists but is not owned by the
 * requesting user. Both map to {@code 404 EXPENSE_NOT_FOUND} so the response
 * does not reveal whether another user's id exists.
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException() {
        super("Expense not found");
    }
}
