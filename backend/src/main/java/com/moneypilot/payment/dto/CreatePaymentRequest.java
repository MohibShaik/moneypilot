package com.moneypilot.payment.dto;

import com.moneypilot.payment.PaymentType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request to record a payment. The target id required depends on {@code type}
 * (the service enforces the shape): {@code LOAN_EMI} needs {@code loanEmiId};
 * {@code CREDIT_CARD} needs {@code cardStatementId}; {@code OTHER} needs
 * neither. {@code paymentMethodId} is optional for any type. {@code paidAt}
 * defaults to now when omitted.
 */
public record CreatePaymentRequest(

        @NotNull
        PaymentType type,

        @NotNull
        @Positive
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        OffsetDateTime paidAt,

        UUID loanEmiId,

        UUID cardStatementId,

        UUID paymentMethodId
) {
}
