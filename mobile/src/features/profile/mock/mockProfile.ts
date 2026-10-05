import type { UserProfile } from '../types';

/**
 * Mock profile for UI-first development (HLD §20). Isolated so it can be
 * replaced by the real `GET /api/v1/users/me` response without touching the
 * screens. Contains no password hash or sensitive auth data (HLD §24).
 */
export const mockProfile: UserProfile = {
  id: '7f000000-0000-0000-0000-0000000000e2',
  email: 'alex@example.com',
  displayName: 'Alex',
  currency: 'INR',
  timezone: 'Asia/Kolkata',
  createdAt: '2026-08-01T10:00:00Z',
};
