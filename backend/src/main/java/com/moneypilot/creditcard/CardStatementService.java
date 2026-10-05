package com.moneypilot.creditcard;

import com.moneypilot.common.BadRequestException;
import com.moneypilot.creditcard.dto.CardStatementResponse;
import com.moneypilot.creditcard.dto.CreateCardStatementRequest;
import com.moneypilot.creditcard.dto.UpdateCardStatementRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for credit card statements.
 *
 * <p>Ownership is always enforced through the parent card: every operation
 * first resolves an owned {@link CreditCard} via the JWT-subject user id. A
 * statement that belongs to a non-owned (or missing) card is reported as not
 * found. The statement status is derived from the paid amount unless the caller
 * supplies an explicit status (so OVERDUE can be set). Cross-field rules are
 * validated before persisting so the client gets a clear 400 rather than a
 * generic integrity error, with the DB CHECKs as the backstop.
 */
@Service
public class CardStatementService {

    /** V2 unique constraint on {@code (credit_card_id, statement_date)}. */
    static final String CARD_DATE_UNIQUE_CONSTRAINT = "uq_card_statements_card_date";

    private final CreditCardRepository cardRepository;
    private final CardStatementRepository statementRepository;
    private final CardStatementMapper mapper;

    public CardStatementService(CreditCardRepository cardRepository,
                                CardStatementRepository statementRepository,
                                CardStatementMapper mapper) {
        this.cardRepository = cardRepository;
        this.statementRepository = statementRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CardStatementResponse> list(UUID userId, UUID cardId) {
        requireOwnedCard(userId, cardId);
        return statementRepository.findByCreditCardIdOrderByStatementDateDesc(cardId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public CardStatementResponse create(UUID userId, UUID cardId, CreateCardStatementRequest request) {
        requireOwnedCard(userId, cardId);

        BigDecimal paidAmount = request.paidAmount() != null
                ? request.paidAmount()
                : BigDecimal.ZERO;
        validateAmounts(request.statementDate(), request.dueDate(),
                request.statementAmount(), request.minimumDue(), paidAmount);

        StatementStatus status = request.status() != null
                ? request.status()
                : deriveStatus(request.statementAmount(), paidAmount);

        CardStatement statement = new CardStatement(
                cardId,
                request.statementDate(),
                request.dueDate(),
                request.statementAmount(),
                request.minimumDue(),
                paidAmount,
                status);

        try {
            return mapper.toResponse(statementRepository.saveAndFlush(statement));
        } catch (DataIntegrityViolationException ex) {
            if (isCardDateUniqueViolation(ex)) {
                throw new DuplicateCardStatementException();
            }
            throw ex;
        }
    }

    @Transactional
    public CardStatementResponse update(UUID userId, UUID cardId, UUID statementId,
                                        UpdateCardStatementRequest request) {
        requireOwnedCard(userId, cardId);
        CardStatement statement = statementRepository.findByIdAndCreditCardId(statementId, cardId)
                .orElseThrow(CardStatementNotFoundException::new);

        validateAmounts(statement.getStatementDate(), request.dueDate(),
                request.statementAmount(), request.minimumDue(), request.paidAmount());

        statement.setDueDate(request.dueDate());
        statement.setStatementAmount(request.statementAmount());
        statement.setMinimumDue(request.minimumDue());
        statement.setPaidAmount(request.paidAmount());
        statement.setStatus(request.status() != null
                ? request.status()
                : deriveStatus(request.statementAmount(), request.paidAmount()));

        return mapper.toResponse(statementRepository.saveAndFlush(statement));
    }

    @Transactional
    public void delete(UUID userId, UUID cardId, UUID statementId) {
        requireOwnedCard(userId, cardId);
        CardStatement statement = statementRepository.findByIdAndCreditCardId(statementId, cardId)
                .orElseThrow(CardStatementNotFoundException::new);
        statementRepository.delete(statement);
    }

    private void requireOwnedCard(UUID userId, UUID cardId) {
        cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(CreditCardNotFoundException::new);
    }

    /**
     * Derives status from the paid amount relative to the statement amount.
     * Public so the Payments module can reuse the same rule when settling.
     */
    public static StatementStatus deriveStatus(BigDecimal statementAmount, BigDecimal paidAmount) {
        if (paidAmount.signum() <= 0) {
            return StatementStatus.GENERATED;
        }
        if (paidAmount.compareTo(statementAmount) >= 0) {
            return StatementStatus.PAID;
        }
        return StatementStatus.PARTIALLY_PAID;
    }

    private void validateAmounts(LocalDate statementDate, LocalDate dueDate,
                                 BigDecimal statementAmount, BigDecimal minimumDue,
                                 BigDecimal paidAmount) {
        if (dueDate.isBefore(statementDate)) {
            throw new BadRequestException("dueDate must not be before statementDate");
        }
        if (minimumDue.compareTo(BigDecimal.ZERO) < 0
                || minimumDue.compareTo(statementAmount) > 0) {
            throw new BadRequestException("minimumDue must be between 0 and statementAmount");
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0
                || paidAmount.compareTo(statementAmount) > 0) {
            throw new BadRequestException("paidAmount must be between 0 and statementAmount");
        }
    }

    private boolean isCardDateUniqueViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                String name = cve.getConstraintName();
                return name != null && name.toLowerCase().contains(CARD_DATE_UNIQUE_CONSTRAINT);
            }
            cause = cause.getCause();
        }
        return false;
    }
}
