package com.moneypilot.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneypilot.loan.dto.CreateLoanRequest;
import com.moneypilot.loan.dto.LoanResponse;
import com.moneypilot.loan.dto.UpdateLoanRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoanServiceTest {

    private LoanRepository loanRepository;
    private LoanEmiRepository emiRepository;
    private LoanService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID loanId = UUID.randomUUID();
    private final LocalDate start = LocalDate.of(2026, 1, 1);

    @BeforeEach
    void setUp() {
        loanRepository = mock(LoanRepository.class);
        emiRepository = mock(LoanEmiRepository.class);
        service = new LoanService(loanRepository, emiRepository,
                new LoanScheduleGenerator(), new LoanMapper());
    }

    private CreateLoanRequest createRequest(BigDecimal emiAmount) {
        return new CreateLoanRequest(
                "Home Loan", "HDFC", LoanType.HOME,
                new BigDecimal("100000.00"), new BigDecimal("12.000"),
                start, 12, emiAmount);
    }

    @Test
    void createPersistsLoanThenSchedule() {
        when(loanRepository.saveAndFlush(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emiRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse response = service.create(userId, createRequest(null));

        verify(loanRepository).saveAndFlush(any(Loan.class));
        verify(emiRepository).saveAll(anyList());
        assertThat(response.name()).isEqualTo("Home Loan");
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.outstandingPrincipal()).isEqualByComparingTo("100000.00");
        assertThat(response.endDate()).isEqualTo(start.plusMonths(12));
        assertThat(response.emis()).hasSize(12);
        // EMI is computed when not supplied.
        assertThat(response.emiAmount().signum()).isPositive();
    }

    @Test
    void createHonorsClientSuppliedEmi() {
        when(loanRepository.saveAndFlush(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emiRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse response = service.create(userId, createRequest(new BigDecimal("9000.00")));

        assertThat(response.emiAmount()).isEqualByComparingTo("9000.00");
    }

    @Test
    void getThrowsNotFoundWhenNotOwned() {
        when(loanRepository.findByIdAndUserId(loanId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId, loanId))
                .isInstanceOf(LoanNotFoundException.class);
        verify(emiRepository, never()).findByLoanIdOrderByInstallmentNoAsc(any());
    }

    @Test
    void getReturnsDetailWithSchedule() {
        Loan loan = sampleLoan();
        when(loanRepository.findByIdAndUserId(loanId, userId)).thenReturn(Optional.of(loan));
        when(emiRepository.findByLoanIdOrderByInstallmentNoAsc(any()))
                .thenReturn(List.of(sampleEmi()));

        LoanResponse response = service.get(userId, loanId);

        assertThat(response.emis()).hasSize(1);
        assertThat(response.name()).isEqualTo("Home Loan");
    }

    @Test
    void updateThrowsNotFoundWhenNotOwned() {
        when(loanRepository.findByIdAndUserId(loanId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(userId, loanId,
                new UpdateLoanRequest("New name", "ICICI", LoanStatus.CLOSED)))
                .isInstanceOf(LoanNotFoundException.class);
        verify(loanRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateChangesOnlyNameLenderStatus() {
        Loan loan = sampleLoan();
        when(loanRepository.findByIdAndUserId(loanId, userId)).thenReturn(Optional.of(loan));
        when(loanRepository.saveAndFlush(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emiRepository.findByLoanIdOrderByInstallmentNoAsc(any())).thenReturn(List.of());

        LoanResponse response = service.update(userId, loanId,
                new UpdateLoanRequest("Renamed", "ICICI", LoanStatus.CLOSED));

        assertThat(response.name()).isEqualTo("Renamed");
        assertThat(response.lender()).isEqualTo("ICICI");
        assertThat(response.status()).isEqualTo(LoanStatus.CLOSED);
        // Financial terms untouched.
        assertThat(response.principalAmount()).isEqualByComparingTo("100000.00");
        assertThat(response.tenureMonths()).isEqualTo(12);
    }

    @Test
    void deleteThrowsNotFoundWhenNotOwned() {
        when(loanRepository.findByIdAndUserId(loanId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, loanId))
                .isInstanceOf(LoanNotFoundException.class);
        verify(emiRepository, never()).deleteByLoanId(any());
        verify(loanRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesEmisBeforeLoan() {
        Loan loan = sampleLoan();
        when(loanRepository.findByIdAndUserId(loanId, userId)).thenReturn(Optional.of(loan));

        service.delete(userId, loanId);

        verify(emiRepository).deleteByLoanId(eq(loan.getId()));
        verify(loanRepository).delete(eq(loan));
    }

    private Loan sampleLoan() {
        return new Loan(userId, "Home Loan", "HDFC", LoanType.HOME,
                new BigDecimal("100000.00"), new BigDecimal("8884.88"),
                new BigDecimal("12.000"), start, start.plusMonths(12), 12);
    }

    private LoanEmi sampleEmi() {
        return new LoanEmi(loanId, 1, new BigDecimal("7884.88"),
                new BigDecimal("1000.00"), new BigDecimal("8884.88"), start.plusMonths(1));
    }
}
