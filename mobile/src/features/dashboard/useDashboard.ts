import { useCallback, useEffect, useState } from 'react';

import { mockDashboard } from './mock/mockDashboard';
import type { DashboardData } from './types';

/**
 * Data hook for the dashboard (HLD §18, §21).
 *
 * This abstraction is the seam between the screen and the data source. Today it
 * resolves isolated mock data after a short delay; when the backend is ready it
 * will be reimplemented with TanStack Query against `/api/v1/dashboard`, keeping
 * the same return shape so the screen does not change (HLD §21).
 *
 * The screen never knows whether the data is mock or API-backed.
 */
export type UseDashboardResult = {
  data: DashboardData | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

/** Simulated latency so loading states are exercised during UI-first dev. */
const MOCK_DELAY_MS = 600;

export function useDashboard(): UseDashboardResult {
  const [data, setData] = useState<DashboardData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const load = useCallback(() => {
    setIsLoading(true);
    setIsError(false);
    const timer = setTimeout(() => {
      // Mock resolution. Replace with a TanStack Query fetch later.
      setData(mockDashboard);
      setIsLoading(false);
    }, MOCK_DELAY_MS);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    const cancel = load();
    return cancel;
  }, [load]);

  return { data, isLoading, isError, refetch: load };
}
