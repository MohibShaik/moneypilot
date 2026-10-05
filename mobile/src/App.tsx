import { StatusBar } from 'expo-status-bar';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { RootNavigator } from '@/navigation/RootNavigator';
import { appTheme } from '@/theme/theme';

/**
 * Root application component.
 *
 * Providers (safe area + Paper theme) wrap the root navigator, which switches
 * between the auth stack (Login/Register) and the main app based on session
 * state. The first screen is Login.
 */
export default function App() {
  return (
    <SafeAreaProvider>
      <PaperProvider theme={appTheme}>
        <RootNavigator />
        <StatusBar style="auto" />
      </PaperProvider>
    </SafeAreaProvider>
  );
}
