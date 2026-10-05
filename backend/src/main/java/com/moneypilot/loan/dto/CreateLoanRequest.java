package com.moneypilot.loan.dto;

import com.moneypilot.loan.LoanType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to create a loan. The EMI schedule is generated server-side. When
 * {@code emiAmount} is omitted, the service computes it from principal, rate,
 * and tenure (reducing-balance). Financial terms are immutable after creation.
 */
public record CreateLoanRequest(

        @NotBlank
        @Size(min = 1, max = 150)
        String name,

        @Size(max = 150)
        String lender,

        @NotNull
        LoanType loanType,

        @NotNull
        @Positive
        @Digits(integer = 13, fraction = 2)
        BigDecimal principalAmount,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 3, fraction = 3)
        BigDecimal interestRate,

        @NotNull
        LocalDate startDate,

        @NotNull
        @Min(1)
        @Max(600)
        Integer tenureMonths,

        @Positive
        @Digits(integer = 13, fraction = 2)
        BigDecimal emiAmount
) {
}
