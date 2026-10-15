import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

/**
 * Secure storage for the JWT access token (HLD §24).
 *
 * On native (iOS/Android) this uses expo-secure-store (Keychain / Keystore).
 * On web, expo-secure-store has no implementation (its native module is empty),
 * so we fall back to localStorage, which is appropriate for local web dev.
 * The token is never kept in plain Zustand state.
 */
const TOKEN_KEY = 'moneypilot.accessToken';
const isWeb = Platform.OS === 'web';

/** Persists the access token. */
export async function saveToken(token: string): Promise<void> {
  if (isWeb) {
    globalThis.localStorage?.setItem(TOKEN_KEY, token);
    return;
  }
  await SecureStore.setItemAsync(TOKEN_KEY, token);
}

/** Returns the stored access token, or null if none. */
export async function getToken(): Promise<string | null> {
  if (isWeb) {
    return globalThis.localStorage?.getItem(TOKEN_KEY) ?? null;
  }
  return SecureStore.getItemAsync(TOKEN_KEY);
}

/** Deletes the stored access token (sign-out). */
export async function clearToken(): Promise<void> {
  if (isWeb) {
    globalThis.localStorage?.removeItem(TOKEN_KEY);
    return;
  }
  await SecureStore.deleteItemAsync(TOKEN_KEY);
}
