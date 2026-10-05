package com.moneypilot.expense;

import com.moneypilot.expense.dto.ExpenseResponse;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Expense} entities to their public {@link ExpenseResponse}.
 */
@Component
public class ExpenseMapper {

    public ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getExpenseDate(),
                expense.getDescription(),
                expense.getNotes(),
                expense.getCategoryId(),
                expense.getPaymentMethodId(),
                expense.getCreatedAt());
    }
}
