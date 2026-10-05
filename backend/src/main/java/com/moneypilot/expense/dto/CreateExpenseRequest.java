package com.moneypilot.expense.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to create an expense. {@code categoryId} and {@code paymentMethodId}
 * are optional; when supplied the service validates that the category is
 * visible to the user (system or owned) and the payment method is owned.
 */
public record CreateExpenseRequest(

        @NotNull
        @Positive
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @NotNull
        LocalDate expenseDate,

        @Size(max = 500)
        String description,

        @Size(max = 2000)
        String notes,

        UUID categoryId,

        UUID paymentMethodId
) {
}
