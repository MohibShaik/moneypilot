package com.moneypilot.paymentmethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneypilot.common.BadRequestException;
import com.moneypilot.creditcard.CreditCard;
import com.moneypilot.creditcard.CreditCardNotFoundException;
import com.moneypilot.creditcard.CreditCardRepository;
import com.moneypilot.paymentmethod.dto.CreatePaymentMethodRequest;
import com.moneypilot.paymentmethod.dto.UpdatePaymentMethodRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentMethodServiceTest {

    private PaymentMethodRepository repository;
    private CreditCardRepository creditCardRepository;
    private PaymentMethodService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID pmId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(PaymentMethodRepository.class);
        creditCardRepository = mock(CreditCardRepository.class);
        service = new PaymentMethodService(repository, creditCardRepository, new PaymentMethodMapper());
    }

    @Test
    void createsNonCreditCardMethodWithNullCardId() {
        when(repository.saveAndFlush(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(userId,
                new CreatePaymentMethodRequest(PaymentMethodType.UPI, "My UPI", null));

        assertThat(response.type()).isEqualTo(PaymentMethodType.UPI);
        assertThat(response.label()).isEqualTo("My UPI");
        assertThat(response.creditCardId()).isNull();
    }

    @Test
    void createLinksOwnedCreditCard() {
        when(creditCardRepository.findByIdAndUserId(cardId, userId))
                .thenReturn(Optional.of(mock(CreditCard.class)));
        when(repository.saveAndFlush(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(userId,
                new CreatePaymentMethodRequest(PaymentMethodType.CREDIT_CARD, "HDFC", cardId));

        assertThat(response.type()).isEqualTo(PaymentMethodType.CREDIT_CARD);
        assertThat(response.creditCardId()).isEqualTo(cardId);
    }

    @Test
    void createCreditCardWithoutCardIdIsBadRequest() {
        assertThatThrownBy(() -> service.create(userId,
                new CreatePaymentMethodRequest(PaymentMethodType.CREDIT_CARD, "HDFC", null)))
                .isInstanceOf(BadRequestException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createCreditCardWithNonOwnedCardIsNotFound() {
        when(creditCardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(userId,
                new CreatePaymentMethodRequest(PaymentMethodType.CREDIT_CARD, "HDFC", cardId)))
                .isInstanceOf(CreditCardNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createNonCreditCardWithStrayCardIdIsBadRequest() {
        assertThatThrownBy(() -> service.create(userId,
                new CreatePaymentMethodRequest(PaymentMethodType.UPI, "My UPI", cardId)))
                .isInstanceOf(BadRequestException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateThrowsNotFoundWhenNotOwned() {
        when(repository.findByIdAndUserId(pmId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(userId, pmId,
                new UpdatePaymentMethodRequest(PaymentMethodType.CASH, "Cash", null)))
                .isInstanceOf(PaymentMethodNotFoundException.class);
    }

    @Test
    void updatesOwnedMethod() {
        PaymentMethod owned = new PaymentMethod(userId, PaymentMethodType.UPI, "Old");
        when(repository.findByIdAndUserId(pmId, userId)).thenReturn(Optional.of(owned));
        when(repository.saveAndFlush(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.update(userId, pmId,
                new UpdatePaymentMethodRequest(PaymentMethodType.CASH, "Cash", null));

        assertThat(response.type()).isEqualTo(PaymentMethodType.CASH);
        assertThat(response.label()).isEqualTo("Cash");
        assertThat(response.creditCardId()).isNull();
    }

    @Test
    void deleteThrowsNotFoundWhenNotOwned() {
        when(repository.findByIdAndUserId(pmId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, pmId))
                .isInstanceOf(PaymentMethodNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteRemovesOwnedMethod() {
        PaymentMethod owned = new PaymentMethod(userId, PaymentMethodType.CASH, "Cash");
        when(repository.findByIdAndUserId(pmId, userId)).thenReturn(Optional.of(owned));

        service.delete(userId, pmId);

        verify(repository).delete(eq(owned));
    }
}
