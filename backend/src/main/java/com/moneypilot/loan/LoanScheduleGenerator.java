package com.moneypilot.loan;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Generates a loan's EMI (installment) schedule using standard reducing-balance
 * amortization.
 *
 * <p>Every produced row satisfies the DB CHECK
 * {@code principal_component + interest_component = emi_amount} <b>exactly</b>
 * at scale 2, and the principal components sum to the loan principal. Rounding
 * residuals are absorbed into the final installment. Zero-interest loans are
 * supported (all interest components are 0).
 */
@Component
public class LoanScheduleGenerator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    /**
     * Computes the level EMI for a loan via the reducing-balance formula.
     * Returns {@code principal / tenure} (rounded) for a zero-interest loan.
     *
     * @param principal    loan principal (> 0)
     * @param annualRatePct annual interest rate as a percentage (>= 0)
     * @param tenureMonths  number of monthly installments (> 0)
     * @return the level EMI amount at scale 2
     */
    public BigDecimal computeEmi(BigDecimal principal, BigDecimal annualRatePct, int tenureMonths) {
        BigDecimal monthlyRate = monthlyRate(annualRatePct);
        if (monthlyRate.signum() == 0) {
            return money(principal.divide(BigDecimal.valueOf(tenureMonths), MC));
        }
        // E = P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate, MC);
        BigDecimal pow = onePlusR.pow(tenureMonths, MC);
        BigDecimal numerator = principal.multiply(monthlyRate, MC).multiply(pow, MC);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE, MC);
        return money(numerator.divide(denominator, MC));
    }

    /**
     * Builds the full installment schedule. The returned {@link LoanEmi} rows
     * have no id/timestamps (DB-generated on save) and {@code loanId} set to the
     * given parent.
     *
     * @param loanId        parent loan id
     * @param principal     loan principal (> 0)
     * @param annualRatePct annual interest rate as a percentage (>= 0)
     * @param emi           level EMI to use (as computed or client-supplied)
     * @param tenureMonths  number of installments (> 0)
     * @param startDate     loan start date; installment k is due startDate+k months
     * @return the ordered list of installment rows (size == tenureMonths)
     */
    public List<LoanEmi> generate(UUID loanId, BigDecimal principal, BigDecimal annualRatePct,
                                  BigDecimal emi, int tenureMonths, LocalDate startDate) {
        BigDecimal monthlyRate = monthlyRate(annualRatePct);
        BigDecimal balance = money(principal);
        BigDecimal levelEmi = money(emi);
        List<LoanEmi> rows = new ArrayList<>(tenureMonths);

        for (int k = 1; k <= tenureMonths; k++) {
            LocalDate dueDate = startDate.plusMonths(k);
            BigDecimal interest = money(balance.multiply(monthlyRate, MC));

            BigDecimal principalComponent;
            BigDecimal rowEmi;
            if (k == tenureMonths) {
                // Final installment absorbs any residual: pay off the remaining
                // balance exactly, and let the EMI equal principal + interest.
                principalComponent = balance;
                rowEmi = principalComponent.add(interest);
            } else {
                principalComponent = levelEmi.subtract(interest);
                // Clamp: never negative, never more than the outstanding balance.
                if (principalComponent.signum() < 0) {
                    principalComponent = BigDecimal.ZERO.setScale(MONEY_SCALE);
                } else if (principalComponent.compareTo(balance) > 0) {
                    principalComponent = balance;
                }
                rowEmi = principalComponent.add(interest);
            }

            rows.add(new LoanEmi(loanId, k, principalComponent, interest, rowEmi, dueDate));
            balance = balance.subtract(principalComponent);
        }
        return rows;
    }

    private BigDecimal monthlyRate(BigDecimal annualRatePct) {
        if (annualRatePct == null || annualRatePct.signum() == 0) {
            return BigDecimal.ZERO;
        }
        // annual% -> monthly fraction: pct / 100 / 12
        return annualRatePct.divide(HUNDRED, MC).divide(TWELVE, MC);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }
}
