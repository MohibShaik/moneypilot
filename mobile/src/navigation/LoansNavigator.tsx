import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { IconButton } from 'react-native-paper';

import { LoanDetailsScreen, LoanFormScreen, LoanListScreen } from '@/features/loans';
import { palette } from '@/theme/theme';
import type { LoansStackParamList } from './types';

const Stack = createNativeStackNavigator<LoansStackParamList>();

/**
 * Loans stack (HLD §6, §11), nested inside the Loans tab:
 * list → details, plus an add/edit form presented modally.
 */
export function LoansNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen
        name="LoanList"
        component={LoanListScreen}
        options={({ navigation }) => ({
          title: 'Loans',
          headerRight: () => (
            <IconButton
              icon="plus"
              iconColor={palette.accent}
              accessibilityLabel="Add loan"
              onPress={() => navigation.navigate('LoanForm')}
            />
          ),
        })}
      />
      <Stack.Screen
        name="LoanDetails"
        component={LoanDetailsScreen}
        options={({ navigation, route }) => ({
          title: 'Loan details',
          headerRight: () => (
            <IconButton
              icon="pencil"
              iconColor={palette.accent}
              accessibilityLabel="Edit loan"
              onPress={() => navigation.navigate('LoanForm', { loanId: route.params.loanId })}
            />
          ),
        })}
      />
      <Stack.Screen
        name="LoanForm"
        component={LoanFormScreen}
        options={({ route }) => ({
          title: route.params?.loanId ? 'Edit loan' : 'Add loan',
          presentation: 'modal',
        })}
      />
    </Stack.Navigator>
  );
}
