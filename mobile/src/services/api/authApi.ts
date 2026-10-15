import { http } from './client';

/**
 * Auth API (services/api layer). Thin typed wrappers over the auth and
 * profile endpoints. The shared {@link http} client already unwraps the
 * envelope, so these return the `data` payload directly.
 */

export type LoginRequest = {
  email: string;
  password: string;
};

/** Mirrors the backend TokenResponse DTO. */
export type TokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
};

/** Request body for registration. Optional fields are omitted when blank. */
export type RegisterRequest = {
  displayName: string;
  email: string;
  password: string;
  currency?: string;
  timezone?: string;
};

/** Mirrors the backend UserResponse DTO (public profile, no token). */
export type UserResponse = {
  id: string;
  email: string;
  displayName: string;
  currency: string;
  timezone: string;
  createdAt: string;
};

/**
 * POST /auth/login — verifies credentials and returns a JWT access token.
 * On failure the client throws a normalized ApiError.
 */
export async function login(body: LoginRequest): Promise<TokenResponse> {
  const res = await http.post<TokenResponse>('/auth/login', body);
  return res.data;
}

/**
 * POST /auth/register — creates a user and returns the public profile.
 * Does NOT issue a token; the user logs in afterwards. Empty optional
 * fields are stripped so the backend applies its defaults.
 */
export async function register(body: RegisterRequest): Promise<UserResponse> {
  const payload: RegisterRequest = {
    displayName: body.displayName,
    email: body.email,
    password: body.password,
  };
  if (body.currency && body.currency.trim() !== '') payload.currency = body.currency.trim();
  if (body.timezone && body.timezone.trim() !== '') payload.timezone = body.timezone.trim();
  const res = await http.post<UserResponse>('/auth/register', payload);
  return res.data;
}

/**
 * GET /users/me — returns the authenticated user's own profile.
 * Requires the Bearer token (attached by the client interceptor).
 */
export async function getMe(): Promise<UserResponse> {
  const res = await http.get<UserResponse>('/users/me');
  return res.data;
}
