import { useCallback, useEffect, useState } from 'react';

import { findMockLoanDetail, mockLoans } from './mock/mockLoans';
import type { LoanDetail, LoanSummary } from './types';

/**
 * Data hooks for loans (HLD §18, §21). These are the seam between screens and
 * the data source: mock-backed today, reimplementable with TanStack Query
 * against `/api/v1/loans` later without changing the screens. Screens never
 * know whether the data is mock or API-backed.
 */

const MOCK_DELAY_MS = 500;

export type UseLoansResult = {
  data: LoanSummary[] | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useLoans(): UseLoansResult {
  const [data, setData] = useState<LoanSummary[] | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const load = useCallback(() => {
    setIsLoading(true);
    setIsError(false);
    const timer = setTimeout(() => {
      setData(mockLoans);
      setIsLoading(false);
    }, MOCK_DELAY_MS);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => load(), [load]);

  return { data, isLoading, isError, refetch: load };
}

export type UseLoanResult = {
  data: LoanDetail | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useLoan(loanId: string): UseLoanResult {
  const [data, setData] = useState<LoanDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const load = useCallback(() => {
    setIsLoading(true);
    setIsError(false);
    const timer = setTimeout(() => {
      const detail = findMockLoanDetail(loanId);
      if (detail) {
        setData(detail);
      } else {
        setIsError(true);
      }
      setIsLoading(false);
    }, MOCK_DELAY_MS);
    return () => clearTimeout(timer);
  }, [loanId]);

  useEffect(() => load(), [load]);

  return { data, isLoading, isError, refetch: load };
}
