package com.moneypilot.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneypilot.creditcard.dto.CreateCreditCardRequest;
import com.moneypilot.creditcard.dto.CreditCardResponse;
import com.moneypilot.creditcard.dto.UpdateCreditCardRequest;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreditCardServiceTest {

    private CreditCardRepository cardRepository;
    private CardStatementRepository statementRepository;
    private CreditCardService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cardRepository = mock(CreditCardRepository.class);
        statementRepository = mock(CardStatementRepository.class);
        service = new CreditCardService(cardRepository, statementRepository, new CreditCardMapper());
    }

    private CreateCreditCardRequest createRequest() {
        return new CreateCreditCardRequest(
                "Regalia", "HDFC", "1234", new BigDecimal("200000.00"), (short) 5, (short) 25);
    }

    private CreditCard sampleCard() {
        return new CreditCard(userId, "Regalia", "HDFC", "1234",
                new BigDecimal("200000.00"), (short) 5, (short) 25);
    }

    @Test
    void createPersistsActiveCard() {
        when(cardRepository.saveAndFlush(any(CreditCard.class))).thenAnswer(inv -> inv.getArgument(0));

        CreditCardResponse response = service.create(userId, createRequest());

        assertThat(response.cardName()).isEqualTo("Regalia");
        assertThat(response.lastFour()).isEqualTo("1234");
        assertThat(response.status()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void getThrowsNotFoundWhenNotOwned() {
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId, cardId))
                .isInstanceOf(CreditCardNotFoundException.class);
    }

    @Test
    void updateThrowsNotFoundWhenNotOwned() {
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(userId, cardId,
                new UpdateCreditCardRequest("New", "ICICI", new BigDecimal("300000.00"),
                        (short) 10, (short) 28, CardStatus.BLOCKED)))
                .isInstanceOf(CreditCardNotFoundException.class);
        verify(cardRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateChangesMutableFields() {
        CreditCard card = sampleCard();
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));
        when(cardRepository.saveAndFlush(any(CreditCard.class))).thenAnswer(inv -> inv.getArgument(0));

        CreditCardResponse response = service.update(userId, cardId,
                new UpdateCreditCardRequest("Renamed", "ICICI", new BigDecimal("300000.00"),
                        (short) 10, (short) 28, CardStatus.BLOCKED));

        assertThat(response.cardName()).isEqualTo("Renamed");
        assertThat(response.bank()).isEqualTo("ICICI");
        assertThat(response.creditLimit()).isEqualByComparingTo("300000.00");
        assertThat(response.statementDay()).isEqualTo((short) 10);
        assertThat(response.paymentDueDay()).isEqualTo((short) 28);
        assertThat(response.status()).isEqualTo(CardStatus.BLOCKED);
        // lastFour is immutable.
        assertThat(response.lastFour()).isEqualTo("1234");
    }

    @Test
    void deleteThrowsNotFoundWhenNotOwned() {
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, cardId))
                .isInstanceOf(CreditCardNotFoundException.class);
        verify(statementRepository, never()).deleteByCreditCardId(any());
        verify(cardRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesStatementsBeforeCard() {
        CreditCard card = sampleCard();
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));

        service.delete(userId, cardId);

        verify(statementRepository).deleteByCreditCardId(eq(card.getId()));
        verify(cardRepository).delete(eq(card));
    }
}
