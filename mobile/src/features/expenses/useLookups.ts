import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import { listCategories } from '@/services/api/categoryApi';
import { listPaymentMethods } from '@/services/api/paymentMethodApi';
import type { Category, PaymentMethod } from './types';

const STALE_MS = 5 * 60 * 1000;

/** Shared query definitions so the list and the form read the same cache entry. */
export const categoriesQuery = {
  queryKey: ['categories'] as const,
  queryFn: listCategories,
  staleTime: STALE_MS,
};

export const paymentMethodsQuery = {
  queryKey: ['payment-methods'] as const,
  queryFn: listPaymentMethods,
  staleTime: STALE_MS,
};

/** Categories for selection (system + user-owned), from GET /categories. */
export function useCategories(): Category[] {
  const { data } = useQuery(categoriesQuery);
  return useMemo(() => data ?? [], [data]);
}

/** Payment methods for selection, from GET /payment-methods. */
export function usePaymentMethods(): PaymentMethod[] {
  const { data } = useQuery(paymentMethodsQuery);
  return useMemo(
    () => (data ?? []).map((p) => ({ id: p.id, label: p.label, type: p.type })),
    [data],
  );
}
