package com.moneypilot.loan.dto;

import com.moneypilot.loan.EmiStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public representation of a single EMI (installment) in a loan's schedule.
 */
public record LoanEmiResponse(
        UUID id,
        Integer installmentNo,
        BigDecimal principalComponent,
        BigDecimal interestComponent,
        BigDecimal emiAmount,
        LocalDate dueDate,
        LocalDate paidDate,
        EmiStatus status
) {
}
