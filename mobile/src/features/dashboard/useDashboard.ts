import { useQuery } from '@tanstack/react-query';

import { useMe } from '@/hooks/useMe';
import { getDashboard, type DashboardResponse } from '@/services/api/dashboardApi';
import type { DashboardData } from './types';

/**
 * Data hook for the dashboard (HLD §18, §21).
 *
 * This is the seam between the screen and the data source. It fetches the
 * real /api/v1/dashboard endpoint via TanStack Query and adapts the backend
 * DashboardResponse into the DashboardData shape the screen consumes. The
 * greeting name and currency come from the user's profile (GET /users/me).
 */
export type UseDashboardResult = {
  data: DashboardData | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

// The dashboard endpoint provides only the current-month expense total
// (not a loan/card payment breakdown), so those two rows are 0 until the
// backend exposes them. Name and currency come from the profile.
function toDashboardData(
  r: DashboardResponse,
  userName: string,
  currency: string,
): DashboardData {
  const now = new Date();
  const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  return {
    userName,
    overview: {
      totalOutstanding: r.totalOutstandingPrincipal + r.totalOutstandingStatementBalance,
      currency,
    },
    upcomingPayments: r.upcomingDues.map((d) => ({
      id: d.referenceId,
      label: d.label,
      kind: d.type,
      amount: d.amountDue,
      dueDate: d.dueDate,
    })),
    monthlySpending: {
      month,
      expenses: r.currentMonthExpenseTotal,
      loanPayments: 0,
      cardPayments: 0,
    },
    loanSummary: {
      loanCount: r.activeLoanCount,
      totalOutstanding: r.totalOutstandingPrincipal,
    },
    creditCardSummary: {
      cardCount: r.activeCardCount,
      totalOutstanding: r.totalOutstandingStatementBalance,
      totalAvailable: r.totalCreditLimit - r.totalOutstandingStatementBalance,
    },
  };
}

export function useDashboard(): UseDashboardResult {
  const me = useMe();
  const dashboard = useQuery({
    queryKey: ['dashboard'],
    queryFn: getDashboard,
  });

  const userName = me.data?.displayName ?? 'there';
  const currency = me.data?.currency ?? 'INR';

  return {
    data: dashboard.data ? toDashboardData(dashboard.data, userName, currency) : null,
    isLoading: dashboard.isLoading || me.isLoading,
    isError: dashboard.isError || me.isError,
    refetch: () => {
      void dashboard.refetch();
      void me.refetch();
    },
  };
}
