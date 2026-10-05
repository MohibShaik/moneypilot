/**
 * User profile DTO (HLD §15, §20). Mirrors the backend `UserResponse` shape
 * (`GET /api/v1/users/me`) — never includes password hash or any sensitive
 * authentication data (HLD §15, §24).
 */
export type UserProfile = {
  id: string;
  email: string;
  displayName: string;
  currency: string;
  timezone: string;
  /** ISO timestamp. */
  createdAt: string;
};
