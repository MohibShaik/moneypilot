package com.moneypilot.loan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoanScheduleGeneratorTest {

    private final LoanScheduleGenerator generator = new LoanScheduleGenerator();
    private final UUID loanId = UUID.randomUUID();
    private final LocalDate start = LocalDate.of(2026, 1, 1);

    @Test
    void interestBearingScheduleRowsSatisfyComponentSumAndTotalPrincipal() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal ratePct = new BigDecimal("12.000"); // 1% monthly
        int tenure = 12;

        BigDecimal emi = generator.computeEmi(principal, ratePct, tenure);
        List<LoanEmi> rows = generator.generate(loanId, principal, ratePct, emi, tenure, start);

        assertThat(rows).hasSize(tenure);

        BigDecimal principalSum = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            LoanEmi row = rows.get(i);
            // Each row must satisfy the DB CHECK: principal + interest == emi (scale 2).
            assertThat(row.getPrincipalComponent().add(row.getInterestComponent()))
                    .isEqualByComparingTo(row.getEmiAmount());
            assertThat(row.getEmiAmount().signum()).isPositive();
            assertThat(row.getPrincipalComponent().signum()).isGreaterThanOrEqualTo(0);
            assertThat(row.getInterestComponent().signum()).isGreaterThanOrEqualTo(0);
            assertThat(row.getInstallmentNo()).isEqualTo(i + 1);
            assertThat(row.getDueDate()).isEqualTo(start.plusMonths(i + 1L));
            assertThat(row.getStatus()).isEqualTo(EmiStatus.UPCOMING);
            assertThat(row.getLoanId()).isEqualTo(loanId);
            principalSum = principalSum.add(row.getPrincipalComponent());
        }
        // Principal components repay exactly the loan principal.
        assertThat(principalSum).isEqualByComparingTo(principal);
    }

    @Test
    void zeroInterestScheduleHasNoInterestAndRepaysPrincipal() {
        BigDecimal principal = new BigDecimal("12000.00");
        BigDecimal ratePct = new BigDecimal("0.000");
        int tenure = 5;

        BigDecimal emi = generator.computeEmi(principal, ratePct, tenure);
        List<LoanEmi> rows = generator.generate(loanId, principal, ratePct, emi, tenure, start);

        BigDecimal principalSum = BigDecimal.ZERO;
        for (LoanEmi row : rows) {
            assertThat(row.getInterestComponent()).isEqualByComparingTo("0.00");
            assertThat(row.getPrincipalComponent()).isEqualByComparingTo(row.getEmiAmount());
            assertThat(row.getEmiAmount().signum()).isPositive();
            principalSum = principalSum.add(row.getPrincipalComponent());
        }
        assertThat(principalSum).isEqualByComparingTo(principal);
    }

    @Test
    void nonDivisiblePrincipalResidualAbsorbedByFinalInstallment() {
        BigDecimal principal = new BigDecimal("1000.00");
        BigDecimal ratePct = new BigDecimal("0.000");
        int tenure = 3; // 1000/3 = 333.33, residual on last row

        BigDecimal emi = generator.computeEmi(principal, ratePct, tenure);
        List<LoanEmi> rows = generator.generate(loanId, principal, ratePct, emi, tenure, start);

        assertThat(rows.get(0).getEmiAmount()).isEqualByComparingTo("333.33");
        assertThat(rows.get(1).getEmiAmount()).isEqualByComparingTo("333.33");
        // Final row absorbs the residual so the total is exactly the principal.
        assertThat(rows.get(2).getEmiAmount()).isEqualByComparingTo("333.34");

        BigDecimal total = rows.stream()
                .map(LoanEmi::getEmiAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(principal);
    }

    @Test
    void clientSuppliedEmiIsHonoredForNonFinalRows() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal ratePct = new BigDecimal("12.000");
        int tenure = 12;
        BigDecimal clientEmi = new BigDecimal("9000.00");

        List<LoanEmi> rows = generator.generate(loanId, principal, ratePct, clientEmi, tenure, start);

        // Non-final rows use the supplied level EMI.
        assertThat(rows.get(0).getEmiAmount()).isEqualByComparingTo(clientEmi);
        // Component sum still holds for every row.
        for (LoanEmi row : rows) {
            assertThat(row.getPrincipalComponent().add(row.getInterestComponent()))
                    .isEqualByComparingTo(row.getEmiAmount());
        }
    }
}
