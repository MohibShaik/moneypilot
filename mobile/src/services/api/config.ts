import Constants from 'expo-constants';
import { Platform } from 'react-native';

/**
 * Resolves the backend base URL for the current platform.
 *
 * The backend runs locally on port 8080. The reachable host differs by target:
 * - Android emulator reaches the host machine via `10.0.2.2`.
 * - iOS simulator and web can use `localhost`.
 * - A physical device needs the host machine's LAN IP — override by editing
 *   `expo.extra.apiBaseUrl` in app.json (e.g. `http://192.168.1.20:8080`).
 *
 * Values come from `app.json` `extra` so there is a single place to change the
 * URL per environment (including a deployed Render URL later).
 */
type ApiExtra = {
  apiBaseUrl?: string;
  apiBaseUrlAndroid?: string;
};

const extra = (Constants.expoConfig?.extra ?? {}) as ApiExtra;

const DEFAULT_WEB_IOS = 'http://localhost:8080';
const DEFAULT_ANDROID = 'http://10.0.2.2:8080';

function resolveBaseUrl(): string {
  if (Platform.OS === 'android') {
    return extra.apiBaseUrlAndroid ?? DEFAULT_ANDROID;
  }
  return extra.apiBaseUrl ?? DEFAULT_WEB_IOS;
}

/** Host origin, e.g. `http://localhost:8080`. */
export const API_ORIGIN = resolveBaseUrl();

/** Full API base, e.g. `http://localhost:8080/api/v1`. */
export const API_BASE_URL = `${API_ORIGIN}/api/v1`;
