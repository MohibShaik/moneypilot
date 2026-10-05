import { useCallback, useEffect, useMemo, useState } from 'react';

import { mockCategories, mockExpenses, mockPaymentMethods } from './mock/mockExpenses';
import type { Category, Expense, ExpenseGroup, PaymentMethod } from './types';

/**
 * Data hooks for expenses (HLD §18, §21). The seam between screens and the data
 * source: mock-backed now, reimplementable with TanStack Query against
 * `/api/v1/expenses`, `/api/v1/categories`, and payment-method endpoints later
 * without changing the screens.
 */

const MOCK_DELAY_MS = 500;

/** Groups expenses by date (newest first), each group's items newest first. */
function groupByDate(expenses: Expense[]): ExpenseGroup[] {
  const byDate = new Map<string, Expense[]>();
  for (const e of expenses) {
    const bucket = byDate.get(e.expenseDate) ?? [];
    bucket.push(e);
    byDate.set(e.expenseDate, bucket);
  }
  return [...byDate.entries()]
    .sort(([a], [b]) => (a < b ? 1 : -1))
    .map(([date, items]) => ({ date, expenses: items }));
}

export type UseExpensesResult = {
  data: ExpenseGroup[] | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useExpenses(): UseExpensesResult {
  const [raw, setRaw] = useState<Expense[] | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const load = useCallback(() => {
    setIsLoading(true);
    setIsError(false);
    const timer = setTimeout(() => {
      setRaw(mockExpenses);
      setIsLoading(false);
    }, MOCK_DELAY_MS);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => load(), [load]);

  const data = useMemo(() => (raw ? groupByDate(raw) : null), [raw]);

  return { data, isLoading, isError, refetch: load };
}

/** Categories for selection (system + user-owned). Mock-backed for now. */
export function useCategories(): Category[] {
  return mockCategories;
}

/** Payment methods for selection. Mock-backed for now. */
export function usePaymentMethods(): PaymentMethod[] {
  return mockPaymentMethods;
}
