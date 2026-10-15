import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';

import { useMe } from '@/hooks/useMe';
import {
  createExpense,
  getExpense,
  listExpenses,
  updateExpense,
  type ExpenseDto,
  type ExpenseInput,
} from '@/services/api/expenseApi';
import type { ApiError } from '@/types/api';
import type { Expense, ExpenseGroup } from './types';
import { categoriesQuery, paymentMethodsQuery } from './useLookups';

export { useCategories, usePaymentMethods } from './useLookups';

/**
 * Data hooks for expenses (HLD 18, 21). The seam between screens and the API:
 * GET/POST/PUT /api/v1/expenses, joined client-side with categories and
 * payment methods (the backend returns only their ids).
 */

/** Groups expenses by date (newest first), keeping API order within a day. */
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
  const expenses = useQuery({ queryKey: ['expenses'], queryFn: listExpenses });
  const categories = useQuery(categoriesQuery);
  const methods = useQuery(paymentMethodsQuery);
  const me = useMe();

  const data = useMemo(() => {
    if (!expenses.data) return null;
    const categoryNames = new Map((categories.data ?? []).map((c) => [c.id, c.name] as const));
    const methodLabels = new Map((methods.data ?? []).map((p) => [p.id, p.label] as const));
    const currency = me.data?.currency ?? 'INR';
    const rows: Expense[] = expenses.data.map((e) => ({
      id: e.id,
      amount: e.amount,
      expenseDate: e.expenseDate,
      categoryId: e.categoryId,
      categoryName: e.categoryId ? categoryNames.get(e.categoryId) ?? null : null,
      paymentMethodId: e.paymentMethodId,
      paymentMethodLabel: e.paymentMethodId ? methodLabels.get(e.paymentMethodId) ?? null : null,
      description: e.description,
      notes: e.notes,
      currency,
    }));
    return groupByDate(rows);
  }, [expenses.data, categories.data, methods.data, me.data]);

  return {
    data,
    isLoading: expenses.isLoading || categories.isLoading || methods.isLoading || me.isLoading,
    isError: expenses.isError || categories.isError || methods.isError || me.isError,
    refetch: () => {
      void expenses.refetch();
      void categories.refetch();
      void methods.refetch();
      void me.refetch();
    },
  };
}

/** Single expense for the edit form (GET /expenses/{id}). Disabled without an id. */
export function useExpense(id?: string) {
  return useQuery<ExpenseDto, ApiError>({
    queryKey: ['expenses', id],
    queryFn: () => getExpense(id as string),
    enabled: Boolean(id),
  });
}

/**
 * Create (no id) or update (with id) an expense, then refresh everything that
 * derives from expenses: the list, the detail entry, and the dashboard totals.
 */
export function useSaveExpense(expenseId?: string) {
  const queryClient = useQueryClient();
  return useMutation<ExpenseDto, ApiError, ExpenseInput>({
    mutationFn: (body) => (expenseId ? updateExpense(expenseId, body) : createExpense(body)),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['expenses'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ]);
    },
  });
}
