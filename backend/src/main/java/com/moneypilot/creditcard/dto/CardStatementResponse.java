package com.moneypilot.creditcard.dto;

import com.moneypilot.creditcard.StatementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public card-statement representation.
 */
public record CardStatementResponse(
        UUID id,
        UUID creditCardId,
        LocalDate statementDate,
        LocalDate dueDate,
        BigDecimal statementAmount,
        BigDecimal minimumDue,
        BigDecimal paidAmount,
        StatementStatus status,
        OffsetDateTime createdAt
) {
}
