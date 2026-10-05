import { useCallback, useEffect, useState } from 'react';

import { mockProfile } from './mock/mockProfile';
import type { UserProfile } from './types';

/**
 * Data hook for the current user's profile (HLD §18, §21). The seam between the
 * screen and the data source: mock-backed now, reimplementable with TanStack
 * Query against `GET /api/v1/users/me` later without changing the screen.
 */

const MOCK_DELAY_MS = 400;

export type UseProfileResult = {
  data: UserProfile | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useProfile(): UseProfileResult {
  const [data, setData] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const load = useCallback(() => {
    setIsLoading(true);
    setIsError(false);
    const timer = setTimeout(() => {
      setData(mockProfile);
      setIsLoading(false);
    }, MOCK_DELAY_MS);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => load(), [load]);

  return { data, isLoading, isError, refetch: load };
}
