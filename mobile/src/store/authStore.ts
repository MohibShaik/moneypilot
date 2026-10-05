import { create } from 'zustand';

/**
 * Client-side authentication session state (HLD §18).
 *
 * This store holds only the *session status* used to switch between the auth
 * and main navigators. During the UI-first phase it is driven by placeholder
 * sign-in, NOT fake JWT handling (HLD §8): no token is minted or stored here.
 *
 * When real auth is wired (UI TASK 4 / API-integration phase), the access token
 * will be obtained from `POST /api/v1/auth/login` and persisted via secure
 * platform storage (expo-secure-store) — never AsyncStorage, never here in
 * plain client state (HLD §24). The `userEmail` below is display-only context.
 */
export type AuthStatus = 'unauthenticated' | 'authenticated';

type AuthState = {
  status: AuthStatus;
  /** Display-only identifier for the current session (no credentials). */
  userEmail: string | null;
  /** Marks the session active (UI-first placeholder sign-in). */
  signIn: (email: string) => void;
  /** Clears the session. */
  signOut: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  status: 'unauthenticated',
  userEmail: null,
  signIn: (email: string) => set({ status: 'authenticated', userEmail: email }),
  signOut: () => set({ status: 'unauthenticated', userEmail: null }),
}));
