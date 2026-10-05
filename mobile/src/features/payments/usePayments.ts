import { useCallback, useEffect, useMemo, useState } from 'react';

import { findMockPayableTarget, findMockPayment, mockPayments } from './mock/mockPayments';
import type { PayableTarget, Payment, PaymentGroup } from './types';

/**
 * Data hooks for payments (HLD §18, §21). The seam between screens and the data
 * source: mock-backed now, reimplementable with TanStack Query against
 * `/api/v1/payments` later without changing the screens.
 */

const MOCK_DELAY_MS = 500;

function groupByDate(payments: Payment[]): PaymentGroup[] {
  const byDate = new Map<string, Payment[]>();
  for (const p of payments) {
    const bucket = byDate.get(p.paidAt) ?? [];
    bucket.push(p);
    byDate.set(p.paidAt, bucket);
  }
  return [...byDate.entries()]
    .sort(([a], [b]) => (a < b ? 1 : -1))
    .map(([date, items]) => ({ date, payments: items }));
}

export type UsePaymentsResult = {
  data: PaymentGroup[] | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function usePayments(): UsePaymentsResult {
  const [raw, setRaw] = useState<Payment[] | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const load = useCallback(() => {
    setIsLoading(true);
    setIsError(false);
    const timer = setTimeout(() => {
      setRaw(mockPayments);
      setIsLoading(false);
    }, MOCK_DELAY_MS);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => load(), [load]);

  const data = useMemo(() => (raw ? groupByDate(raw) : null), [raw]);
  return { data, isLoading, isError, refetch: load };
}

export type UsePaymentResult = {
  data: Payment | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function usePayment(paymentId: string): UsePaymentResult {
  const [data, setData] = useState<Payment | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const load = useCallback(() => {
    setIsLoading(true);
    setIsError(false);
    const timer = setTimeout(() => {
      const found = findMockPayment(paymentId);
      if (found) setData(found);
      else setIsError(true);
      setIsLoading(false);
    }, MOCK_DELAY_MS);
    return () => clearTimeout(timer);
  }, [paymentId]);

  useEffect(() => load(), [load]);
  return { data, isLoading, isError, refetch: load };
}

export type UsePayableTargetResult = {
  data: PayableTarget | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

/**
 * Loads a payable target (loan EMI or card statement) with its authoritative
 * remaining amount for a payment form.
 */
export function usePayableTarget(targetId: string): UsePayableTargetResult {
  const [data, setData] = useState<PayableTarget | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const load = useCallback(() => {
    setIsLoading(true);
    setIsError(false);
    const timer = setTimeout(() => {
      const found = findMockPayableTarget(targetId);
      if (found) setData(found);
      else setIsError(true);
      setIsLoading(false);
    }, MOCK_DELAY_MS);
    return () => clearTimeout(timer);
  }, [targetId]);

  useEffect(() => load(), [load]);
  return { data, isLoading, isError, refetch: load };
}
