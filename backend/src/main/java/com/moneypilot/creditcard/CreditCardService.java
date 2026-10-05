package com.moneypilot.creditcard;

import com.moneypilot.creditcard.dto.CreateCreditCardRequest;
import com.moneypilot.creditcard.dto.CreditCardResponse;
import com.moneypilot.creditcard.dto.UpdateCreditCardRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for credit cards.
 *
 * <p>Identity always comes from the authenticated user id passed by the
 * controller (the JWT subject). Ownership is enforced on every operation: a
 * user may only read, update, or delete their own cards; a non-owned one is
 * reported as not found. Deleting a card removes its statements first
 * ({@code card_statements.credit_card_id} is ON DELETE RESTRICT).
 */
@Service
public class CreditCardService {

    private final CreditCardRepository cardRepository;
    private final CardStatementRepository statementRepository;
    private final CreditCardMapper mapper;

    public CreditCardService(CreditCardRepository cardRepository,
                             CardStatementRepository statementRepository,
                             CreditCardMapper mapper) {
        this.cardRepository = cardRepository;
        this.statementRepository = statementRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CreditCardResponse> list(UUID userId) {
        return cardRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreditCardResponse get(UUID userId, UUID cardId) {
        return mapper.toResponse(requireOwned(userId, cardId));
    }

    @Transactional
    public CreditCardResponse create(UUID userId, CreateCreditCardRequest request) {
        CreditCard card = new CreditCard(
                userId,
                request.cardName(),
                request.bank(),
                request.lastFour(),
                request.creditLimit(),
                request.statementDay(),
                request.paymentDueDay());
        return mapper.toResponse(cardRepository.saveAndFlush(card));
    }

    @Transactional
    public CreditCardResponse update(UUID userId, UUID cardId, UpdateCreditCardRequest request) {
        CreditCard card = requireOwned(userId, cardId);
        card.setCardName(request.cardName());
        card.setBank(request.bank());
        card.setCreditLimit(request.creditLimit());
        card.setStatementDay(request.statementDay());
        card.setPaymentDueDay(request.paymentDueDay());
        card.setStatus(request.status());
        return mapper.toResponse(cardRepository.saveAndFlush(card));
    }

    @Transactional
    public void delete(UUID userId, UUID cardId) {
        CreditCard card = requireOwned(userId, cardId);
        // card_statements.credit_card_id is ON DELETE RESTRICT: children first.
        statementRepository.deleteByCreditCardId(card.getId());
        statementRepository.flush();
        cardRepository.delete(card);
    }

    private CreditCard requireOwned(UUID userId, UUID cardId) {
        return cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(CreditCardNotFoundException::new);
    }
}
