import { create } from 'zustand';

/**
 * Client-side UI preferences (HLD §18): theme preference and notifications
 * toggle. These are presentation/UX settings only — not financial data. In a
 * later task these may be persisted to secure/async storage; for now they live
 * in memory for the session.
 */
export type ThemePreference = 'light' | 'dark' | 'system';

type UiPrefsState = {
  themePreference: ThemePreference;
  notificationsEnabled: boolean;
  setThemePreference: (pref: ThemePreference) => void;
  setNotificationsEnabled: (enabled: boolean) => void;
};

export const useUiPrefsStore = create<UiPrefsState>((set) => ({
  themePreference: 'light',
  notificationsEnabled: true,
  setThemePreference: (pref) => set({ themePreference: pref }),
  setNotificationsEnabled: (enabled) => set({ notificationsEnabled: enabled }),
}));
