package com.moneypilot.payment;

import com.moneypilot.common.BadRequestException;
import com.moneypilot.creditcard.CardStatement;
import com.moneypilot.creditcard.CardStatementRepository;
import com.moneypilot.creditcard.CardStatementService;
import com.moneypilot.creditcard.CreditCardNotFoundException;
import com.moneypilot.loan.EmiStatus;
import com.moneypilot.loan.LoanEmi;
import com.moneypilot.loan.LoanEmiRepository;
import com.moneypilot.loan.LoanNotFoundException;
import com.moneypilot.payment.dto.CreatePaymentRequest;
import com.moneypilot.payment.dto.PaymentResponse;
import com.moneypilot.paymentmethod.PaymentMethodNotFoundException;
import com.moneypilot.paymentmethod.PaymentMethodRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for payments.
 *
 * <p>Identity always comes from the authenticated user id passed by the
 * controller (the JWT subject). Ownership is enforced on every operation; a
 * non-owned payment is reported as not found.
 *
 * <p>On create the service validates the type/target shape
 * ({@code chk_payments_target}) and the ownership of the referenced target
 * (which the DB FK cannot do), then records the payment and settles the target.
 * Settlement sums all payments against an EMI/statement and updates its paid
 * state and status. Payments are immutable; deleting one re-settles the target.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LoanEmiRepository loanEmiRepository;
    private final CardStatementRepository cardStatementRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMapper mapper;

    public PaymentService(PaymentRepository paymentRepository,
                          LoanEmiRepository loanEmiRepository,
                          CardStatementRepository cardStatementRepository,
                          PaymentMethodRepository paymentMethodRepository,
                          PaymentMapper mapper) {
        this.paymentRepository = paymentRepository;
        this.loanEmiRepository = loanEmiRepository;
        this.cardStatementRepository = cardStatementRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(UUID userId, PaymentType type,
                                      OffsetDateTime from, OffsetDateTime to) {
        return paymentRepository.findForUserFiltered(userId, type, from, to).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID userId, UUID id) {
        return mapper.toResponse(requireOwned(userId, id));
    }

    @Transactional
    public PaymentResponse create(UUID userId, CreatePaymentRequest request) {
        validatePaymentMethod(userId, request.paymentMethodId());
        OffsetDateTime paidAt = request.paidAt() != null ? request.paidAt() : OffsetDateTime.now();

        Payment payment;
        switch (request.type()) {
            case LOAN_EMI -> {
                requireNull(request.cardStatementId(), "cardStatementId must be null for a LOAN_EMI payment");
                UUID emiId = require(request.loanEmiId(), "loanEmiId is required for a LOAN_EMI payment");
                LoanEmi emi = loanEmiRepository.findOwnedByIdAndUserId(emiId, userId)
                        .orElseThrow(LoanNotFoundException::new);
                payment = save(userId, PaymentType.LOAN_EMI, emiId, null,
                        request.paymentMethodId(), request.amount(), paidAt);
                settleEmi(emi);
            }
            case CREDIT_CARD -> {
                requireNull(request.loanEmiId(), "loanEmiId must be null for a CREDIT_CARD payment");
                UUID stmtId = require(request.cardStatementId(),
                        "cardStatementId is required for a CREDIT_CARD payment");
                CardStatement statement = cardStatementRepository.findOwnedByIdAndUserId(stmtId, userId)
                        .orElseThrow(CreditCardNotFoundException::new);
                payment = save(userId, PaymentType.CREDIT_CARD, null, stmtId,
                        request.paymentMethodId(), request.amount(), paidAt);
                settleStatement(statement);
            }
            case OTHER -> {
                requireNull(request.loanEmiId(), "loanEmiId must be null for an OTHER payment");
                requireNull(request.cardStatementId(), "cardStatementId must be null for an OTHER payment");
                payment = save(userId, PaymentType.OTHER, null, null,
                        request.paymentMethodId(), request.amount(), paidAt);
            }
            default -> throw new BadRequestException("Unsupported payment type");
        }
        return mapper.toResponse(payment);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Payment payment = requireOwned(userId, id);
        UUID emiId = payment.getLoanEmiId();
        UUID stmtId = payment.getCardStatementId();

        paymentRepository.delete(payment);
        paymentRepository.flush();

        // Re-settle the affected target from the remaining payment sum.
        if (emiId != null) {
            loanEmiRepository.findById(emiId).ifPresent(this::settleEmi);
        }
        if (stmtId != null) {
            cardStatementRepository.findById(stmtId).ifPresent(this::settleStatement);
        }
    }

    private Payment save(UUID userId, PaymentType type, UUID emiId, UUID stmtId,
                         UUID paymentMethodId, BigDecimal amount, OffsetDateTime paidAt) {
        Payment payment = new Payment(userId, type, emiId, stmtId, paymentMethodId, amount, paidAt);
        Payment saved = paymentRepository.saveAndFlush(payment);
        // Flush so settlement sums include this payment.
        return saved;
    }

    private Payment requireOwned(UUID userId, UUID id) {
        return paymentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(PaymentNotFoundException::new);
    }

    private void validatePaymentMethod(UUID userId, UUID paymentMethodId) {
        if (paymentMethodId != null) {
            paymentMethodRepository.findByIdAndUserId(paymentMethodId, userId)
                    .orElseThrow(PaymentMethodNotFoundException::new);
        }
    }

    /**
     * Settles a loan EMI from the sum of its payments: fully covered → PAID with
     * paid date; otherwise reverts to UPCOMING with no paid date (DUE/OVERDUE
     * are time-based and not auto-assigned here).
     */
    private void settleEmi(LoanEmi emi) {
        BigDecimal total = paymentRepository.sumByLoanEmiId(emi.getId());
        if (total.compareTo(emi.getEmiAmount()) >= 0) {
            emi.setStatus(EmiStatus.PAID);
            emi.setPaidDate(LocalDate.now());
        } else {
            emi.setStatus(EmiStatus.UPCOMING);
            emi.setPaidDate(null);
        }
        loanEmiRepository.saveAndFlush(emi);
    }

    /**
     * Settles a card statement from the sum of its payments: paid amount capped
     * at the statement amount (DB CHECK), status derived with the same rule as
     * the Credit Cards module.
     */
    private void settleStatement(CardStatement statement) {
        BigDecimal total = paymentRepository.sumByCardStatementId(statement.getId());
        BigDecimal paid = total.min(statement.getStatementAmount());
        statement.setPaidAmount(paid);
        statement.setStatus(CardStatementService.deriveStatus(statement.getStatementAmount(), paid));
        cardStatementRepository.saveAndFlush(statement);
    }

    private static UUID require(UUID value, String message) {
        if (value == null) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private static void requireNull(UUID value, String message) {
        if (value != null) {
            throw new BadRequestException(message);
        }
    }
}
