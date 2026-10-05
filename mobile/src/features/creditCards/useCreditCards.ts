import { useCallback, useEffect, useState } from 'react';

import { findMockCardDetail, mockCards } from './mock/mockCards';
import type { CardDetail, CardSummary } from './types';

/**
 * Data hooks for credit cards (HLD §18, §21). The seam between screens and the
 * data source: mock-backed now, reimplementable with TanStack Query against
 * `/api/v1/credit-cards` later without changing the screens.
 */

const MOCK_DELAY_MS = 500;

export type UseCreditCardsResult = {
  data: CardSummary[] | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useCreditCards(): UseCreditCardsResult {
  const [data, setData] = useState<CardSummary[] | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const load = useCallback(() => {
    setIsLoading(true);
    setIsError(false);
    const timer = setTimeout(() => {
      setData(mockCards);
      setIsLoading(false);
    }, MOCK_DELAY_MS);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => load(), [load]);

  return { data, isLoading, isError, refetch: load };
}

export type UseCreditCardResult = {
  data: CardDetail | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useCreditCard(cardId: string): UseCreditCardResult {
  const [data, setData] = useState<CardDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const load = useCallback(() => {
    setIsLoading(true);
    setIsError(false);
    const timer = setTimeout(() => {
      const detail = findMockCardDetail(cardId);
      if (detail) {
        setData(detail);
      } else {
        setIsError(true);
      }
      setIsLoading(false);
    }, MOCK_DELAY_MS);
    return () => clearTimeout(timer);
  }, [cardId]);

  useEffect(() => load(), [load]);

  return { data, isLoading, isError, refetch: load };
}
