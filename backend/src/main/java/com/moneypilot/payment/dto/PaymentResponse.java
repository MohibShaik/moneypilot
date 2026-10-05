package com.moneypilot.payment.dto;

import com.moneypilot.payment.PaymentType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public payment representation.
 */
public record PaymentResponse(
        UUID id,
        PaymentType type,
        BigDecimal amount,
        OffsetDateTime paidAt,
        UUID loanEmiId,
        UUID cardStatementId,
        UUID paymentMethodId,
        OffsetDateTime createdAt
) {
}
