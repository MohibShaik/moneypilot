import axios, { AxiosError, type AxiosInstance } from 'axios';

import type { ApiError, ApiResponse } from '@/types/api';
import { API_BASE_URL } from './config';

/**
 * Shared axios instance for all backend calls (services/api layer).
 *
 * Responsiblities:
 * - Base URL from config ({@link API_BASE_URL}).
 * - Unwrap the standard { success, data, error } envelope so callers get `data`.
 * - Normalize failures (envelope error, HTTP error, network error) into a
 *   single {@link ApiError} that is thrown, so callers have one error shape.
 *
 * The Authorization header is attached per-call by an injected token getter
 * (see {@link setAuthTokenGetter}) so this module has no storage dependency.
 */

let authTokenGetter: (() => Promise<string | null>) | null = null;

/** Registers how the client obtains the current bearer token. */
export function setAuthTokenGetter(getter: () => Promise<string | null>): void {
  authTokenGetter = getter;
}

let onUnauthorized: (() => void) | null = null;

/** Registers a callback invoked on a 401 (e.g. to clear the session). */
export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler;
}

export const http: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// Attach the bearer token (when present) to every request.
http.interceptors.request.use(async (config) => {
  if (authTokenGetter) {
    const token = await authTokenGetter();
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`);
    }
  }
  return config;
});

/** Normalizes any thrown failure into an {@link ApiError}. */
function toApiError(error: unknown): ApiError {
  if (error instanceof AxiosError) {
    const envelope = error.response?.data as ApiResponse<unknown> | undefined;
    if (envelope?.error) return envelope.error;
    if (error.code === 'ECONNABORTED') {
      return { code: 'TIMEOUT', message: 'The request timed out. Please try again.' };
    }
    if (!error.response) {
      return { code: 'NETWORK_ERROR', message: 'Cannot reach the server. Check your connection.' };
    }
    return { code: 'HTTP_' + error.response.status, message: 'Something went wrong. Please try again.' };
  }
  return { code: 'UNKNOWN', message: 'Something went wrong. Please try again.' };
}

// Unwrap the envelope on success; normalize and rethrow on failure.
http.interceptors.response.use(
  (response) => {
    const envelope = response.data as ApiResponse<unknown>;
    if (envelope && typeof envelope === 'object' && 'success' in envelope) {
      if (envelope.success) {
        response.data = envelope.data;
        return response;
      }
      return Promise.reject(envelope.error ?? { code: 'UNKNOWN', message: 'Request failed.' });
    }
    return response;
  },
  (error) => {
    if (error instanceof AxiosError && error.response?.status === 401 && onUnauthorized) {
      onUnauthorized();
    }
    return Promise.reject(toApiError(error));
  },
);
