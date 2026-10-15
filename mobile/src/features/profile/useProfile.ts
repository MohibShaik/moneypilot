import { useMe } from '@/hooks/useMe';
import type { UserProfile } from './types';

/**
 * Data hook for the current user's profile (HLD §18, §21). The seam between the
 * screen and the data source: fetches the real `GET /api/v1/users/me` endpoint
 * via {@link useMe} (TanStack Query). The backend UserResponse matches the
 * UserProfile shape exactly, so no adaptation is needed and the screen stays
 * unchanged.
 */

export type UseProfileResult = {
  data: UserProfile | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useProfile(): UseProfileResult {
  const me = useMe();
  return {
    data: me.data ?? null,
    isLoading: me.isLoading,
    isError: me.isError,
    refetch: () => {
      void me.refetch();
    },
  };
}
