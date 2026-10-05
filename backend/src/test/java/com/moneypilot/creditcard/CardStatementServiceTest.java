package com.moneypilot.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneypilot.common.BadRequestException;
import com.moneypilot.creditcard.dto.CardStatementResponse;
import com.moneypilot.creditcard.dto.CreateCardStatementRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardStatementServiceTest {

    private CreditCardRepository cardRepository;
    private CardStatementRepository statementRepository;
    private CardStatementService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();
    private final LocalDate stmtDate = LocalDate.of(2026, 1, 5);
    private final LocalDate dueDate = LocalDate.of(2026, 1, 25);

    @BeforeEach
    void setUp() {
        cardRepository = mock(CreditCardRepository.class);
        statementRepository = mock(CardStatementRepository.class);
        service = new CardStatementService(cardRepository, statementRepository, new CardStatementMapper());
    }

    private void cardOwned() {
        when(cardRepository.findByIdAndUserId(cardId, userId))
                .thenReturn(Optional.of(mock(CreditCard.class)));
    }

    private CreateCardStatementRequest req(BigDecimal amount, BigDecimal minDue,
                                           BigDecimal paid, StatementStatus status) {
        return new CreateCardStatementRequest(stmtDate, dueDate, amount, minDue, paid, status);
    }

    @Test
    void createRequiresOwnedCard() {
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(userId, cardId,
                req(new BigDecimal("1000.00"), new BigDecimal("100.00"), null, null)))
                .isInstanceOf(CreditCardNotFoundException.class);
        verify(statementRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDerivesGeneratedWhenUnpaid() {
        cardOwned();
        when(statementRepository.saveAndFlush(any(CardStatement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CardStatementResponse response = service.create(userId, cardId,
                req(new BigDecimal("1000.00"), new BigDecimal("100.00"), null, null));

        assertThat(response.status()).isEqualTo(StatementStatus.GENERATED);
        assertThat(response.paidAmount()).isEqualByComparingTo("0");
    }

    @Test
    void createDerivesPartiallyPaid() {
        cardOwned();
        when(statementRepository.saveAndFlush(any(CardStatement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CardStatementResponse response = service.create(userId, cardId,
                req(new BigDecimal("1000.00"), new BigDecimal("100.00"), new BigDecimal("400.00"), null));

        assertThat(response.status()).isEqualTo(StatementStatus.PARTIALLY_PAID);
    }

    @Test
    void createDerivesPaidWhenFullyPaid() {
        cardOwned();
        when(statementRepository.saveAndFlush(any(CardStatement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CardStatementResponse response = service.create(userId, cardId,
                req(new BigDecimal("1000.00"), new BigDecimal("100.00"), new BigDecimal("1000.00"), null));

        assertThat(response.status()).isEqualTo(StatementStatus.PAID);
    }

    @Test
    void createHonorsExplicitStatus() {
        cardOwned();
        when(statementRepository.saveAndFlush(any(CardStatement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CardStatementResponse response = service.create(userId, cardId,
                req(new BigDecimal("1000.00"), new BigDecimal("100.00"), null, StatementStatus.OVERDUE));

        assertThat(response.status()).isEqualTo(StatementStatus.OVERDUE);
    }

    @Test
    void createRejectsDueDateBeforeStatementDate() {
        cardOwned();
        CreateCardStatementRequest bad = new CreateCardStatementRequest(
                stmtDate, stmtDate.minusDays(1),
                new BigDecimal("1000.00"), new BigDecimal("100.00"), null, null);

        assertThatThrownBy(() -> service.create(userId, cardId, bad))
                .isInstanceOf(BadRequestException.class);
        verify(statementRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsMinimumDueAboveStatementAmount() {
        cardOwned();

        assertThatThrownBy(() -> service.create(userId, cardId,
                req(new BigDecimal("1000.00"), new BigDecimal("2000.00"), null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createRejectsPaidAmountAboveStatementAmount() {
        cardOwned();

        assertThatThrownBy(() -> service.create(userId, cardId,
                req(new BigDecimal("1000.00"), new BigDecimal("100.00"), new BigDecimal("1500.00"), null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deriveStatusLogic() {
        assertThat(CardStatementService.deriveStatus(new BigDecimal("1000"), new BigDecimal("0")))
                .isEqualTo(StatementStatus.GENERATED);
        assertThat(CardStatementService.deriveStatus(new BigDecimal("1000"), new BigDecimal("500")))
                .isEqualTo(StatementStatus.PARTIALLY_PAID);
        assertThat(CardStatementService.deriveStatus(new BigDecimal("1000"), new BigDecimal("1000")))
                .isEqualTo(StatementStatus.PAID);
    }
}
