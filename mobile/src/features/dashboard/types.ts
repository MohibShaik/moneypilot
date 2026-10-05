/**
 * Dashboard DTO types (HLD §10, §20). Shaped to resemble the eventual
 * `/api/v1/dashboard` response so screens and the data hook stay stable when
 * mock data is replaced by the real API. All monetary values are
 * backend-computed amounts (the client never calculates authoritative totals,
 * HLD §4.1).
 */

export type PaymentKind = 'LOAN_EMI' | 'CREDIT_CARD' | 'OTHER';

export type UpcomingPayment = {
  id: string;
  /** What the payment is for, e.g. "Home Loan" or "HDFC Card". */
  label: string;
  kind: PaymentKind;
  amount: number;
  /** ISO date (YYYY-MM-DD). */
  dueDate: string;
};

export type MonthlySpending = {
  /** e.g. "2026-10" — the month this summary covers. */
  month: string;
  expenses: number;
  loanPayments: number;
  cardPayments: number;
};

export type LoanSummary = {
  loanCount: number;
  totalOutstanding: number;
};

export type CreditCardSummary = {
  cardCount: number;
  totalOutstanding: number;
  totalAvailable: number;
};

export type FinancialOverview = {
  /** Total outstanding across all obligations (backend-computed). */
  totalOutstanding: number;
  currency: string;
};

export type DashboardData = {
  /** Display name for the greeting. */
  userName: string;
  overview: FinancialOverview;
  upcomingPayments: UpcomingPayment[];
  monthlySpending: MonthlySpending;
  loanSummary: LoanSummary;
  creditCardSummary: CreditCardSummary;
};
