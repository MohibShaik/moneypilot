package com.moneypilot.creditcard.dto;

import com.moneypilot.creditcard.StatementStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to create a card statement. {@code paidAmount} defaults to 0 when
 * omitted. {@code status} is optional: when omitted the service derives it from
 * {@code paidAmount} vs {@code statementAmount}; when supplied it is honored
 * (so OVERDUE can be set). Cross-field rules (dueDate >= statementDate, amount
 * ranges) are validated by the service.
 */
public record CreateCardStatementRequest(

        @NotNull
        LocalDate statementDate,

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

        @PositiveOrZero
        @Digits(integer = 13, fraction = 2)
        BigDecimal paidAmount,

        StatementStatus status
) {
}
