import { Ionicons } from '@expo/vector-icons';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';

import { DashboardScreen } from '@/features/dashboard';
import { palette } from '@/theme/theme';
import { CardsNavigator } from './CardsNavigator';
import { ExpensesNavigator } from './ExpensesNavigator';
import { LoansNavigator } from './LoansNavigator';
import { MoreNavigator } from './MoreNavigator';
import type { MainTabParamList } from './types';

const Tab = createBottomTabNavigator<MainTabParamList>();

/** Line icons per tab, matching the reference icon set. */
const TAB_ICON: Record<keyof MainTabParamList, keyof typeof Ionicons.glyphMap> = {
  Home: 'home-outline',
  Loans: 'cash-outline',
  Cards: 'card-outline',
  Expenses: 'receipt-outline',
  More: 'ellipsis-horizontal',
};

/**
 * Main app bottom-tab navigator (HLD §6): Home, Loans, Cards, Expenses, More.
 * Shown while the session is authenticated. Tabs whose content is a nested stack
 * hide the tab header to avoid a double header.
 */
export function MainNavigator() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: true,
        tabBarActiveTintColor: palette.accent,
        tabBarInactiveTintColor: palette.textSecondary,
        tabBarIcon: ({ color, size }) => (
          <Ionicons name={TAB_ICON[route.name]} size={size} color={color} />
        ),
      })}
    >
      <Tab.Screen name="Home" component={DashboardScreen} options={{ title: 'Home' }} />
      <Tab.Screen name="Loans" component={LoansNavigator} options={{ headerShown: false }} />
      <Tab.Screen name="Cards" component={CardsNavigator} options={{ headerShown: false }} />
      <Tab.Screen name="Expenses" component={ExpensesNavigator} options={{ headerShown: false }} />
      <Tab.Screen name="More" component={MoreNavigator} options={{ headerShown: false }} />
    </Tab.Navigator>
  );
}
