package com.moneypilot.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated financial summary for the authenticated user's home screen. All
 * monetary sums are non-null (default {@code 0}); counts default to {@code 0};
 * {@code upcomingDues} defaults to an empty list.
 */
public record DashboardResponse(
        long activeLoanCount,
        BigDecimal totalOutstandingPrincipal,
        BigDecimal totalMonthlyEmi,
        long activeCardCount,
        BigDecimal totalCreditLimit,
        BigDecimal totalOutstandingStatementBalance,
        BigDecimal currentMonthExpenseTotal,
        long currentMonthExpenseCount,
        List<UpcomingDueItem> upcomingDues
) {
}
