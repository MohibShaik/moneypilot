package com.moneypilot.loan;

import com.moneypilot.loan.dto.LoanEmiResponse;
import com.moneypilot.loan.dto.LoanResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Loan}/{@link LoanEmi} entities to their public response records.
 */
@Component
public class LoanMapper {

    /** Loan summary without the EMI schedule (list endpoint). */
    public LoanResponse toSummary(Loan loan) {
        return toResponse(loan, null);
    }

    /** Loan detail including its EMI schedule (detail endpoint). */
    public LoanResponse toDetail(Loan loan, List<LoanEmi> emis) {
        return toResponse(loan, emis.stream().map(this::toEmiResponse).toList());
    }

    private LoanResponse toResponse(Loan loan, List<LoanEmiResponse> emis) {
        return new LoanResponse(
                loan.getId(),
                loan.getName(),
                loan.getLender(),
                loan.getLoanType(),
                loan.getPrincipalAmount(),
                loan.getOutstandingPrincipal(),
                loan.getEmiAmount(),
                loan.getInterestRate(),
                loan.getStartDate(),
                loan.getEndDate(),
                loan.getTenureMonths(),
                loan.getStatus(),
                loan.getCreatedAt(),
                emis);
    }

    public LoanEmiResponse toEmiResponse(LoanEmi emi) {
        return new LoanEmiResponse(
                emi.getId(),
                emi.getInstallmentNo(),
                emi.getPrincipalComponent(),
                emi.getInterestComponent(),
                emi.getEmiAmount(),
                emi.getDueDate(),
                emi.getPaidDate(),
                emi.getStatus());
    }
}
