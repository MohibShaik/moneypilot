package com.moneypilot.creditcard.dto;

import com.moneypilot.creditcard.StatementStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to update a card statement. {@code statementDate} is immutable (it is
 * the unique key with the card) and is not accepted here. {@code status} is
 * optional with the same derive-or-honor rule as creation.
 */
public record UpdateCardStatementRequest(

        @NotNull
        LocalDate dueDate,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 13, fraction = 2)
        BigDecimal statementAmount,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 13, fraction = 2)
        BigDecimal minimumDue,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 13, fraction = 2)
        BigDecimal paidAmount,

        StatementStatus status
) {
}
