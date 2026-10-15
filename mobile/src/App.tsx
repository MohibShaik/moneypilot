import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StatusBar } from 'expo-status-bar';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { ToastHost } from '@/components';
import { RootNavigator } from '@/navigation/RootNavigator';
import { setAuthTokenGetter, setUnauthorizedHandler } from '@/services/api/client';
import { clearToken, getToken } from '@/services/auth/tokenStorage';
import { useAuthStore } from '@/store/authStore';
import { appTheme } from '@/theme/theme';

const queryClient = new QueryClient();

// Wire the API client to session concerns once, at module load:
// - how to get the bearer token (from secure storage)
// - what to do on a 401 (clear token + sign out)
setAuthTokenGetter(getToken);
setUnauthorizedHandler(() => {
  void clearToken();
  useAuthStore.getState().signOut();
});

/**
 * Root application component.
 *
 * Providers (Query client + safe area + Paper theme) wrap the root
 * navigator, which switches between the auth stack (Login/Register) and the
 * main app based on session state. The first screen is Login.
 */
export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <SafeAreaProvider>
        <PaperProvider theme={appTheme}>
          <RootNavigator />
          <ToastHost />
          <StatusBar style="auto" />
        </PaperProvider>
      </SafeAreaProvider>
    </QueryClientProvider>
  );
}
