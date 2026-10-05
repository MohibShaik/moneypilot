import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { LoginScreen, RegisterScreen } from '@/features/auth';
import type { AuthStackParamList } from './types';

const Stack = createNativeStackNavigator<AuthStackParamList>();

/**
 * Authentication stack (HLD §6): Login and Register. Shown while the session is
 * unauthenticated.
 */
export function AuthNavigator() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="Register" component={RegisterScreen} />
    </Stack.Navigator>
  );
}
