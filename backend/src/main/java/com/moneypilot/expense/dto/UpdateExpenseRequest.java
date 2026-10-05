package com.moneypilot.expense.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to update an expense. Replaces the mutable fields. {@code categoryId}
 * and {@code paymentMethodId} are optional and validated as on create (null
 * clears the reference).
 */
public record UpdateExpenseRequest(

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
