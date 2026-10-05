package com.moneypilot.creditcard;

import com.moneypilot.creditcard.dto.CardStatementResponse;
import org.springframework.stereotype.Component;

/**
 * Maps {@link CardStatement} entities to their public
 * {@link CardStatementResponse}.
 */
@Component
public class CardStatementMapper {

    public CardStatementResponse toResponse(CardStatement statement) {
        return new CardStatementResponse(
                statement.getId(),
                statement.getCreditCardId(),
                statement.getStatementDate(),
                statement.getDueDate(),
                statement.getStatementAmount(),
                statement.getMinimumDue(),
                statement.getPaidAmount(),
                statement.getStatus(),
                statement.getCreatedAt());
    }
}
