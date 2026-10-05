package com.moneypilot.loan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moneypilot.loan.LoanStatus;
import com.moneypilot.loan.LoanType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public loan representation. {@code emis} carries the full EMI schedule on the
 * detail endpoint and is omitted from the list endpoint (null → not serialized).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoanResponse(
        UUID id,
        String name,
        String lender,
        LoanType loanType,
        BigDecimal principalAmount,
        BigDecimal outstandingPrincipal,
        BigDecimal emiAmount,
        BigDecimal interestRate,
        LocalDate startDate,
        LocalDate endDate,
        Integer tenureMonths,
        LoanStatus status,
        OffsetDateTime createdAt,
        List<LoanEmiResponse> emis
) {
}
