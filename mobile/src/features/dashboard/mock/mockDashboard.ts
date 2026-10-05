import type { DashboardData } from '../types';

/**
 * Mock dashboard data for UI-first development (HLD §20). Isolated here so it
 * can be swapped for the real `/api/v1/dashboard` response without touching the
 * screen. Values resemble the eventual API DTO; they are illustrative only and
 * are never computed on the client (HLD §4.1).
 */
export const mockDashboard: DashboardData = {
  userName: 'Alex',
  overview: {
    totalOutstanding: 3930000,
    currency: 'INR',
  },
  upcomingPayments: [
    { id: 'up-1', label: 'Home Loan', kind: 'LOAN_EMI', amount: 28500, dueDate: '2026-10-10' },
    { id: 'up-2', label: 'HDFC Card', kind: 'CREDIT_CARD', amount: 12500, dueDate: '2026-10-15' },
  ],
  monthlySpending: {
    month: '2026-10',
    expenses: 32500,
    loanPayments: 28500,
    cardPayments: 15000,
  },
  loanSummary: {
    loanCount: 1,
    totalOutstanding: 3250000,
  },
  creditCardSummary: {
    cardCount: 1,
    totalOutstanding: 50000,
    totalAvailable: 135000,
  },
};

/** An empty dashboard, used to exercise the empty state during development. */
export const mockEmptyDashboard: DashboardData = {
  userName: 'Alex',
  overview: { totalOutstanding: 0, currency: 'INR' },
  upcomingPayments: [],
  monthlySpending: { month: '2026-10', expenses: 0, loanPayments: 0, cardPayments: 0 },
  loanSummary: { loanCount: 0, totalOutstanding: 0 },
  creditCardSummary: { cardCount: 0, totalOutstanding: 0, totalAvailable: 0 },
};
