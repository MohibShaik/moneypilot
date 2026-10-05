package com.moneypilot.creditcard.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request to create a credit card. Status defaults to ACTIVE server-side.
 */
public record CreateCreditCardRequest(

        @NotBlank
        @Size(min = 1, max = 120)
        String cardName,

        @NotBlank
        @Size(min = 1, max = 100)
        String bank,

        @NotNull
        @Pattern(regexp = "^[0-9]{4}$", message = "must be exactly 4 digits")
        String lastFour,

        @NotNull
        @Positive
        @Digits(integer = 13, fraction = 2)
        BigDecimal creditLimit,

        @NotNull
        @Min(1)
        @Max(31)
        Short statementDay,

        @NotNull
        @Min(1)
        @Max(31)
        Short paymentDueDay
) {
}
