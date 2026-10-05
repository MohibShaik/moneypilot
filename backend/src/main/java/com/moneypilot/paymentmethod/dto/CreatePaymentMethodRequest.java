package com.moneypilot.paymentmethod.dto;

import com.moneypilot.paymentmethod.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to create a payment method. When {@code type == CREDIT_CARD},
 * {@code creditCardId} must reference a credit card owned by the caller; the
 * service validates ownership. For all other types {@code creditCardId} must be
 * null.
 */
public record CreatePaymentMethodRequest(

        @NotNull
        PaymentMethodType type,

        @NotBlank
        @Size(min = 1, max = 100)
        String label,

        UUID creditCardId
) {
}
