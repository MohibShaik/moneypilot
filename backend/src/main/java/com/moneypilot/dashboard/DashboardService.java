package com.moneypilot.dashboard;

import com.moneypilot.creditcard.CardStatement;
import com.moneypilot.creditcard.CardStatementRepository;
import com.moneypilot.creditcard.CreditCard;
import com.moneypilot.creditcard.CreditCardRepository;
import com.moneypilot.dashboard.dto.DashboardResponse;
import com.moneypilot.dashboard.dto.UpcomingDueItem;
import com.moneypilot.expense.ExpenseRepository;
import com.moneypilot.loan.Loan;
import com.moneypilot.loan.LoanEmi;
import com.moneypilot.loan.LoanEmiRepository;
import com.moneypilot.loan.LoanRepository;
import com.moneypilot.payment.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the read-only dashboard summary for a user by aggregating across the
 * loans, credit-card, and expense modules. Every figure is scoped to the given
 * (JWT-subject) user id. No writes occur.
 */
@Service
public class DashboardService {

    /** Maximum number of merged upcoming-due items returned. */
    private static final int UPCOMING_DUES_LIMIT = 10;

    private final LoanRepository loanRepository;
    private final LoanEmiRepository loanEmiRepository;
    private final CreditCardRepository creditCardRepository;
    private final CardStatementRepository cardStatementRepository;
    private final ExpenseRepository expenseRepository;

    public DashboardService(LoanRepository loanRepository,
                            LoanEmiRepository loanEmiRepository,
                            CreditCardRepository creditCardRepository,
                            CardStatementRepository cardStatementRepository,
                            ExpenseRepository expenseRepository) {
        this.loanRepository = loanRepository;
        this.loanEmiRepository = loanEmiRepository;
        this.creditCardRepository = creditCardRepository;
        this.cardStatementRepository = cardStatementRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getSummary(UUID userId) {
        // Loans (ACTIVE only).
        long activeLoanCount = loanRepository.countActiveByUserId(userId);
        BigDecimal totalOutstandingPrincipal = loanRepository.sumOutstandingPrincipalActive(userId);
        BigDecimal totalMonthlyEmi = loanRepository.sumMonthlyEmiActive(userId);

        // Credit cards (ACTIVE only) + outstanding statement balance.
        long activeCardCount = creditCardRepository.countActiveByUserId(userId);
        BigDecimal totalCreditLimit = creditCardRepository.sumCreditLimitActive(userId);
        BigDecimal totalOutstandingStatementBalance =
                cardStatementRepository.sumOutstandingForUser(userId);

        // Expenses in the current calendar month.
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        BigDecimal currentMonthExpenseTotal =
                expenseRepository.sumForUserBetween(userId, monthStart, monthEnd);
        long currentMonthExpenseCount =
                expenseRepository.countForUserBetween(userId, monthStart, monthEnd);

        List<UpcomingDueItem> upcomingDues = buildUpcomingDues(userId);

        return new DashboardResponse(
                activeLoanCount,
                totalOutstandingPrincipal,
                totalMonthlyEmi,
                activeCardCount,
                totalCreditLimit,
                totalOutstandingStatementBalance,
                currentMonthExpenseTotal,
                currentMonthExpenseCount,
                upcomingDues);
    }

    private List<UpcomingDueItem> buildUpcomingDues(UUID userId) {
        // Name maps for labeling, loaded once to avoid per-row lookups.
        Map<UUID, String> loanNames = loanRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .collect(Collectors.toMap(Loan::getId, Loan::getName));
        Map<UUID, String> cardNames = creditCardRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .collect(Collectors.toMap(CreditCard::getId, CreditCard::getCardName));

        List<UpcomingDueItem> items = new ArrayList<>();

        for (LoanEmi emi : loanEmiRepository.findUnpaidForUser(userId)) {
            String loanName = loanNames.getOrDefault(emi.getLoanId(), "Loan");
            items.add(new UpcomingDueItem(
                    PaymentType.LOAN_EMI,
                    emi.getId(),
                    loanName + " — EMI #" + emi.getInstallmentNo(),
                    emi.getDueDate(),
                    emi.getEmiAmount()));
        }

        for (CardStatement statement : cardStatementRepository.findUnsettledForUser(userId)) {
            String cardName = cardNames.getOrDefault(statement.getCreditCardId(), "Credit card");
            BigDecimal amountDue = statement.getStatementAmount().subtract(statement.getPaidAmount());
            items.add(new UpcomingDueItem(
                    PaymentType.CREDIT_CARD,
                    statement.getId(),
                    cardName,
                    statement.getDueDate(),
                    amountDue));
        }

        return items.stream()
                .sorted(Comparator.comparing(UpcomingDueItem::dueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(UPCOMING_DUES_LIMIT)
                .toList();
    }
}
