import { NavigationContainer } from '@react-navigation/native';

import { useAuthStore } from '@/store/authStore';
import { AuthNavigator } from './AuthNavigator';
import { MainNavigator } from './MainNavigator';

/**
 * Root navigator (HLD §6). Owns the NavigationContainer and switches between the
 * authentication stack and the main app based on the session status held in the
 * Zustand auth store.
 */
export function RootNavigator() {
  const status = useAuthStore((s) => s.status);
  return (
    <NavigationContainer>
      {status === 'authenticated' ? <MainNavigator /> : <AuthNavigator />}
    </NavigationContainer>
  );
}
