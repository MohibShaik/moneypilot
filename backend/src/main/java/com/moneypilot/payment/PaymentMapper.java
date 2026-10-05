package com.moneypilot.payment;

import com.moneypilot.payment.dto.PaymentResponse;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Payment} entities to their public {@link PaymentResponse}.
 */
@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentType(),
                payment.getAmount(),
                payment.getPaidAt(),
                payment.getLoanEmiId(),
                payment.getCardStatementId(),
                payment.getPaymentMethodId(),
                payment.getCreatedAt());
    }
}
