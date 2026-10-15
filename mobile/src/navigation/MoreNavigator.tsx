import { Ionicons } from '@expo/vector-icons';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Divider, List } from 'react-native-paper';

import { AppButton } from '@/components/AppButton';
import {
  CardPaymentScreen,
  LoanEmiPaymentScreen,
  PaymentDetailsScreen,
  PaymentHistoryScreen,
} from '@/features/payments';
import { PaymentMethodsScreen } from '@/features/paymentMethods';
import { EditProfileScreen, ProfileScreen, SettingsScreen } from '@/features/profile';
import { useAuthStore } from '@/store/authStore';
import { palette, spacing } from '@/theme/theme';
import type { MoreStackParamList } from './types';

const Stack = createNativeStackNavigator<MoreStackParamList>();

type MoreHomeProps = NativeStackScreenProps<MoreStackParamList, 'MoreHome'>;

/**
 * "More" landing menu (HLD §6): entry points to Payments, Profile, and Settings,
 * plus logout. Profile and Settings screens are built in UI TASK 10; for now
 * their rows are present but inert.
 */
function MoreHomeScreen({ navigation }: MoreHomeProps) {
  const signOut = useAuthStore((s) => s.signOut);
  return (
    <ScrollView contentContainerStyle={styles.content}>
      <List.Section>
        <List.Item
          title="Profile"
          left={(p) => <List.Icon {...p} icon="account-outline" />}
          right={() => <Ionicons name="chevron-forward" size={18} color={palette.textSecondary} />}
          onPress={() => navigation.navigate('Profile')}
        />
        <Divider />
        <List.Item
          title="Payment history"
          left={(p) => <List.Icon {...p} icon="history" />}
          right={() => <Ionicons name="chevron-forward" size={18} color={palette.textSecondary} />}
          onPress={() => navigation.navigate('PaymentHistory')}
        />
        <Divider />
        <List.Item
          title="Payment methods"
          left={(p) => <List.Icon {...p} icon="credit-card-outline" />}
          right={() => <Ionicons name="chevron-forward" size={18} color={palette.textSecondary} />}
          onPress={() => navigation.navigate('PaymentMethods')}
        />
        <Divider />
        <List.Item
          title="Settings"
          left={(p) => <List.Icon {...p} icon="cog-outline" />}
          right={() => <Ionicons name="chevron-forward" size={18} color={palette.textSecondary} />}
          onPress={() => navigation.navigate('Settings')}
        />
      </List.Section>
      <View style={styles.logout}>
        <AppButton label="Log out" variant="secondary" onPress={signOut} />
      </View>
    </ScrollView>
  );
}

/**
 * More stack (HLD §6), nested inside the More tab: landing menu plus payment
 * history, payment details, and the loan-EMI / card payment forms.
 */
export function MoreNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="MoreHome" component={MoreHomeScreen} options={{ title: 'More' }} />
      <Stack.Screen
        name="PaymentHistory"
        component={PaymentHistoryScreen}
        options={{ title: 'Payments' }}
      />
      <Stack.Screen
        name="PaymentDetails"
        component={PaymentDetailsScreen}
        options={{ title: 'Payment details' }}
      />
      <Stack.Screen
        name="LoanEmiPayment"
        component={LoanEmiPaymentScreen}
        options={{ title: 'Pay EMI', presentation: 'modal' }}
      />
      <Stack.Screen
        name="CardPayment"
        component={CardPaymentScreen}
        options={{ title: 'Pay card', presentation: 'modal' }}
      />
      <Stack.Screen
        name="PaymentMethods"
        component={PaymentMethodsScreen}
        options={{ title: 'Payment methods' }}
      />
      <Stack.Screen name="Profile" component={ProfileScreen} options={{ title: 'Profile' }} />
      <Stack.Screen
        name="EditProfile"
        component={EditProfileScreen}
        options={{ title: 'Edit profile', presentation: 'modal' }}
      />
      <Stack.Screen name="Settings" component={SettingsScreen} options={{ title: 'Settings' }} />
    </Stack.Navigator>
  );
}

const styles = StyleSheet.create({
  content: { paddingVertical: spacing.sm },
  logout: { padding: spacing.md },
});
