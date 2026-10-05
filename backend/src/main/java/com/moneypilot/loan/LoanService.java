package com.moneypilot.loan;

import com.moneypilot.loan.dto.CreateLoanRequest;
import com.moneypilot.loan.dto.LoanResponse;
import com.moneypilot.loan.dto.UpdateLoanRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for loans and their EMI schedules.
 *
 * <p>Identity always comes from the authenticated user id passed by the
 * controller (the JWT subject). Ownership is enforced on every operation: a
 * user may only read, update, or delete their own loans; a non-owned one is
 * reported as not found.
 *
 * <p>On creation the EMI schedule is generated server-side
 * ({@link LoanScheduleGenerator}) so every installment satisfies the DB
 * component-sum CHECK. Financial terms and the schedule are immutable in V1;
 * {@link #update} only changes name, lender, and status. Deleting a loan
 * removes its EMIs first ({@code loan_emis.loan_id} is ON DELETE RESTRICT).
 */
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanEmiRepository emiRepository;
    private final LoanScheduleGenerator scheduleGenerator;
    private final LoanMapper mapper;

    public LoanService(LoanRepository loanRepository, LoanEmiRepository emiRepository,
                       LoanScheduleGenerator scheduleGenerator, LoanMapper mapper) {
        this.loanRepository = loanRepository;
        this.emiRepository = emiRepository;
        this.scheduleGenerator = scheduleGenerator;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> list(UUID userId) {
        return loanRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanResponse get(UUID userId, UUID loanId) {
        Loan loan = loanRepository.findByIdAndUserId(loanId, userId)
                .orElseThrow(LoanNotFoundException::new);
        List<LoanEmi> emis = emiRepository.findByLoanIdOrderByInstallmentNoAsc(loan.getId());
        return mapper.toDetail(loan, emis);
    }

    @Transactional
    public LoanResponse create(UUID userId, CreateLoanRequest request) {
        BigDecimal emi = request.emiAmount() != null
                ? request.emiAmount()
                : scheduleGenerator.computeEmi(
                        request.principalAmount(), request.interestRate(), request.tenureMonths());

        LocalDate endDate = request.startDate().plusMonths(request.tenureMonths());

        Loan loan = new Loan(
                userId,
                request.name(),
                request.lender(),
                request.loanType(),
                request.principalAmount(),
                emi,
                request.interestRate(),
                request.startDate(),
                endDate,
                request.tenureMonths());
        Loan saved = loanRepository.saveAndFlush(loan);

        List<LoanEmi> schedule = scheduleGenerator.generate(
                saved.getId(),
                request.principalAmount(),
                request.interestRate(),
                emi,
                request.tenureMonths(),
                request.startDate());
        List<LoanEmi> persisted = emiRepository.saveAll(schedule);
        emiRepository.flush();

        return mapper.toDetail(saved, persisted);
    }

    @Transactional
    public LoanResponse update(UUID userId, UUID loanId, UpdateLoanRequest request) {
        Loan loan = loanRepository.findByIdAndUserId(loanId, userId)
                .orElseThrow(LoanNotFoundException::new);
        loan.setName(request.name());
        loan.setLender(request.lender());
        loan.setStatus(request.status());
        Loan saved = loanRepository.saveAndFlush(loan);
        List<LoanEmi> emis = emiRepository.findByLoanIdOrderByInstallmentNoAsc(saved.getId());
        return mapper.toDetail(saved, emis);
    }

    @Transactional
    public void delete(UUID userId, UUID loanId) {
        Loan loan = loanRepository.findByIdAndUserId(loanId, userId)
                .orElseThrow(LoanNotFoundException::new);
        // loan_emis.loan_id is ON DELETE RESTRICT: remove children first.
        emiRepository.deleteByLoanId(loan.getId());
        emiRepository.flush();
        loanRepository.delete(loan);
    }
}
