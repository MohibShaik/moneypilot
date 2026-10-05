package com.moneypilot.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public expense representation.
 */
public record ExpenseResponse(
        UUID id,
        BigDecimal amount,
        LocalDate expenseDate,
        String description,
        String notes,
        UUID categoryId,
        UUID paymentMethodId,
        OffsetDateTime createdAt
) {
}
