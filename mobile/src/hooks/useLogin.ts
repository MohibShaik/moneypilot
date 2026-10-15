import { useMutation } from '@tanstack/react-query';

import type { ApiError } from '@/types/api';
import { login, type LoginRequest, type TokenResponse } from '@/services/api/authApi';
import { saveToken } from '@/services/auth/tokenStorage';
import { useAuthStore } from '@/store/authStore';

/**
 * Login mutation (screen-facing hook).
 *
 * On success: persists the JWT via secure storage, then marks the Zustand
 * session authenticated so the root navigator switches to the main app.
 * On failure: the error is a normalized ApiError the screen can render.
 */
export function useLogin() {
  const signIn = useAuthStore((s) => s.signIn);

  return useMutation<TokenResponse, ApiError, LoginRequest>({
    mutationFn: login,
    onSuccess: async (tokens, variables) => {
      await saveToken(tokens.accessToken);
      signIn(variables.email);
    },
  });
}
