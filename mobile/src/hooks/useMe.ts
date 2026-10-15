import { useQuery } from '@tanstack/react-query';

import { getMe, type UserResponse } from '@/services/api/authApi';

/**
 * Fetches the authenticated user's profile (GET /users/me).
 * Used for the greeting name and the user's currency. Cached under ['me'].
 */
export function useMe() {
  return useQuery<UserResponse>({
    queryKey: ['me'],
    queryFn: getMe,
    staleTime: 5 * 60 * 1000,
  });
}
