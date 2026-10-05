package com.moneypilot.paymentmethod;

import com.moneypilot.paymentmethod.dto.PaymentMethodResponse;
import org.springframework.stereotype.Component;

/**
 * Maps {@link PaymentMethod} entities to their public {@link PaymentMethodResponse}.
 */
@Component
public class PaymentMethodMapper {

    public PaymentMethodResponse toResponse(PaymentMethod pm) {
        return new PaymentMethodResponse(
                pm.getId(),
                pm.getType(),
                pm.getLabel(),
                pm.getCreditCardId(),
                pm.getCreatedAt());
    }
}
