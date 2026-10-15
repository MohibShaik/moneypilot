import { http } from './client';

/**
 * Dashboard API (services/api layer). Mirrors the backend
 * DashboardResponse / UpcomingDueItem DTOs. The shared {@link http} client
 * unwraps the envelope and attaches the Bearer token, so this returns the
 * `data` payload directly.
 */

export type PaymentType = 'LOAN_EMI' | 'CREDIT_CARD' | 'OTHER';

export type UpcomingDueItem = {
  type: PaymentType;
  referenceId: string;
  label: string;
  /** ISO date (YYYY-MM-DD). */
  dueDate: string;
  amountDue: number;
};

/** Mirrors the backend DashboardResponse DTO (all sums/counts non-null). */
export type DashboardResponse = {
  activeLoanCount: number;
  totalOutstandingPrincipal: number;
  totalMonthlyEmi: number;
  activeCardCount: number;
  totalCreditLimit: number;
  totalOutstandingStatementBalance: number;
  currentMonthExpenseTotal: number;
  currentMonthExpenseCount: number;
  upcomingDues: UpcomingDueItem[];
};

/** GET /dashboard — authenticated aggregate summary for the home screen. */
export async function getDashboard(): Promise<DashboardResponse> {
  const res = await http.get<DashboardResponse>('/dashboard');
  return res.data;
}
