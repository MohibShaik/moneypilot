package com.moneypilot.creditcard.dto;

import com.moneypilot.creditcard.CardStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request to update a credit card. {@code lastFour} is immutable (it is part of
 * the card's identity) and is therefore not accepted here.
 */
public record UpdateCreditCardRequest(

        @NotBlank
        @Size(min = 1, max = 120)
        String cardName,

        @NotBlank
        @Size(min = 1, max = 100)
        String bank,

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
        Short paymentDueDay,

        @NotNull
        CardStatus status
) {
}
