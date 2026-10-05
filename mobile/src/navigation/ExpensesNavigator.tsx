import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { IconButton } from 'react-native-paper';

import { ExpenseFormScreen, ExpenseListScreen } from '@/features/expenses';
import { palette } from '@/theme/theme';
import type { ExpensesStackParamList } from './types';

const Stack = createNativeStackNavigator<ExpensesStackParamList>();

/**
 * Expenses stack (HLD §6, §13), nested inside the Expenses tab:
 * list + an add/edit form presented modally.
 */
export function ExpensesNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen
        name="ExpenseList"
        component={ExpenseListScreen}
        options={({ navigation }) => ({
          title: 'Expenses',
          headerRight: () => (
            <IconButton
              icon="plus"
              iconColor={palette.accent}
              accessibilityLabel="Add expense"
              onPress={() => navigation.navigate('ExpenseForm')}
            />
          ),
        })}
      />
      <Stack.Screen
        name="ExpenseForm"
        component={ExpenseFormScreen}
        options={({ route }) => ({
          title: route.params?.expenseId ? 'Edit expense' : 'Add expense',
          presentation: 'modal',
        })}
      />
    </Stack.Navigator>
  );
}
