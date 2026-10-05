/**
 * Shared API envelope types mirroring the backend `ApiResponse` / `ApiError`
 * contract (REST/JSON under `/api/v1`).
 *
 * Backend shape:
 *   success response: { success: true,  data: <T>,  error: null }
 *   failure response: { success: false, data: null, error: ApiError }
 */

export type ApiError = {
  /** Stable machine-readable code, e.g. "VALIDATION_ERROR", "UNAUTHORIZED". */
  code: string;
  /** Human-readable, generic message safe to show to users. */
  message: string;
  /** Present only for validation failures: field name -> message. */
  fieldErrors?: Record<string, string>;
};

export type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  error: ApiError | null;
};
