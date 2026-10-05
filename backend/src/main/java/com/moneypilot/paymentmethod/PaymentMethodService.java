package com.moneypilot.paymentmethod;

import com.moneypilot.common.BadRequestException;
import com.moneypilot.creditcard.CreditCardNotFoundException;
import com.moneypilot.creditcard.CreditCardRepository;
import com.moneypilot.paymentmethod.dto.CreatePaymentMethodRequest;
import com.moneypilot.paymentmethod.dto.PaymentMethodResponse;
import com.moneypilot.paymentmethod.dto.UpdatePaymentMethodRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for payment methods.
 *
 * <p>Identity always comes from the authenticated user id passed by the
 * controller (the JWT subject). Ownership is enforced on every operation: a
 * user may only read, update, or delete their own payment methods; a non-owned
 * one is reported as not found.
 *
 * <p>Credit-card linkage: when {@code type == CREDIT_CARD} the request must
 * carry a {@code creditCardId} referencing a card owned by the same user,
 * validated via {@link CreditCardRepository#findByIdAndUserId}. For all other
 * types the card id must be absent. The DB CHECK
 * {@code chk_payment_methods_credit_card_link} is the structural backstop.
 */
@Service
public class PaymentMethodService {

    private final PaymentMethodRepository repository;
    private final CreditCardRepository creditCardRepository;
    private final PaymentMethodMapper mapper;

    public PaymentMethodService(PaymentMethodRepository repository,
                                CreditCardRepository creditCardRepository,
                                PaymentMethodMapper mapper) {
        this.repository = repository;
        this.creditCardRepository = creditCardRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> list(UUID userId) {
        return repository.findByUserIdOrderByLabelAsc(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public PaymentMethodResponse create(UUID userId, CreatePaymentMethodRequest request) {
        UUID creditCardId = resolveCreditCardId(userId, request.type(), request.creditCardId());
        PaymentMethod pm = new PaymentMethod(userId, request.type(), request.label(), creditCardId);
        return mapper.toResponse(repository.saveAndFlush(pm));
    }

    @Transactional
    public PaymentMethodResponse update(UUID userId, UUID id, UpdatePaymentMethodRequest request) {
        UUID creditCardId = resolveCreditCardId(userId, request.type(), request.creditCardId());
        PaymentMethod pm = repository.findByIdAndUserId(id, userId)
                .orElseThrow(PaymentMethodNotFoundException::new);
        pm.setType(request.type());
        pm.setLabel(request.label());
        pm.setCreditCardId(creditCardId);
        return mapper.toResponse(repository.saveAndFlush(pm));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        PaymentMethod pm = repository.findByIdAndUserId(id, userId)
                .orElseThrow(PaymentMethodNotFoundException::new);
        repository.delete(pm);
    }

    /**
     * Resolves the credit-card id to persist for a payment method.
     *
     * <ul>
     *   <li>{@code CREDIT_CARD}: a non-null {@code creditCardId} is required and
     *       must reference a card owned by the user (else 404 via
     *       {@link CreditCardNotFoundException}); a missing id is a 400.</li>
     *   <li>other types: {@code creditCardId} must be null (a stray id is a
     *       400); returns null.</li>
     * </ul>
     */
    private UUID resolveCreditCardId(UUID userId, PaymentMethodType type, UUID creditCardId) {
        if (type == PaymentMethodType.CREDIT_CARD) {
            if (creditCardId == null) {
                throw new BadRequestException("creditCardId is required for a CREDIT_CARD payment method");
            }
            creditCardRepository.findByIdAndUserId(creditCardId, userId)
                    .orElseThrow(CreditCardNotFoundException::new);
            return creditCardId;
        }
        if (creditCardId != null) {
            throw new BadRequestException("creditCardId is only allowed for a CREDIT_CARD payment method");
        }
        return null;
    }
}
