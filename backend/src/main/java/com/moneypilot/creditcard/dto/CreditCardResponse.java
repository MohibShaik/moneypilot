package com.moneypilot.creditcard.dto;

import com.moneypilot.creditcard.CardStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public credit-card representation.
 */
public record CreditCardResponse(
        UUID id,
        String cardName,
        String bank,
        String lastFour,
        BigDecimal creditLimit,
        Short statementDay,
        Short paymentDueDay,
        CardStatus status,
        OffsetDateTime createdAt
) {
}
