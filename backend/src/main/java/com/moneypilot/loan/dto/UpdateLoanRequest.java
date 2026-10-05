package com.moneypilot.loan.dto;

import com.moneypilot.loan.LoanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to update a loan's mutable fields. Financial terms (principal,
 * interest, tenure, start date) and the generated EMI schedule are immutable in
 * V1, so only {@code name}, {@code lender}, and {@code status} are accepted.
 */
public record UpdateLoanRequest(

        @NotBlank
        @Size(min = 1, max = 150)
        String name,

        @Size(max = 150)
        String lender,

        @NotNull
        LoanStatus status
) {
}
