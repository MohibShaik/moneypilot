package com.moneypilot.paymentmethod.dto;

import com.moneypilot.paymentmethod.PaymentMethodType;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public payment-method representation. {@code creditCardId} is always null in
 * V1 (credit-card linkage is a later capability) but is kept in the contract for
 * forward compatibility.
 */
public record PaymentMethodResponse(
        UUID id,
        PaymentMethodType type,
        String label,
        UUID creditCardId,
        OffsetDateTime createdAt
) {
}
