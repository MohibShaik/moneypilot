package com.moneypilot.creditcard;

import com.moneypilot.creditcard.dto.CreditCardResponse;
import org.springframework.stereotype.Component;

/**
 * Maps {@link CreditCard} entities to their public {@link CreditCardResponse}.
 */
@Component
public class CreditCardMapper {

    public CreditCardResponse toResponse(CreditCard card) {
        return new CreditCardResponse(
                card.getId(),
                card.getCardName(),
                card.getBank(),
                card.getLastFour(),
                card.getCreditLimit(),
                card.getStatementDay(),
                card.getPaymentDueDay(),
                card.getStatus(),
                card.getCreatedAt());
    }
}
