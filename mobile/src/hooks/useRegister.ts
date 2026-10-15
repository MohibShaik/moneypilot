import { useMutation } from '@tanstack/react-query';

import type { ApiError } from '@/types/api';
import {
  register,
  type RegisterRequest,
  type UserResponse,
} from '@/services/api/authApi';

/**
 * Registration mutation (screen-facing hook).
 *
 * Registration returns the public profile and does NOT issue a token, so
 * this hook does no session or token handling -- the screen routes the user
 * to Login on success. On failure the error is a normalized ApiError the
 * screen can render (including per-field validation messages).
 */
export function useRegister() {
  return useMutation<UserResponse, ApiError, RegisterRequest>({
    mutationFn: register,
  });
}
