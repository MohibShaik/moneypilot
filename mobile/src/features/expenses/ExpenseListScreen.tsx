import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Divider, Text, TouchableRipple } from 'react-native-paper';

import { EmptyState, ErrorState, LoadingState, SectionHeader } from '@/components';
import type { ExpensesStackParamList } from '@/navigation/types';
import { palette, spacing } from '@/theme/theme';
import { formatCurrency } from '@/utils/format';
import type { Expense } from './types';
import { useExpenses } from './useExpenses';

type Props = NativeStackScreenProps<ExpensesStackParamList, 'ExpenseList'>;

/** Formats a group's ISO date as a readable header, e.g. "October 3". */
function groupHeading(isoDate: string): string {
  const d = new Date(`${isoDate}T00:00:00`);
  if (Number.isNaN(d.getTime())) return isoDate;
  try {
    return new Intl.DateTimeFormat('en-IN', { month: 'long', day: 'numeric' }).format(d);
  } catch {
    return isoDate;
  }
}

/**
 * Expense list (HLD §13): expenses grouped by date, each row showing category,
 * description, payment method, and amount. Loading/empty/error states (§22).
 * Tapping a row opens the edit form.
 */
export function ExpenseListScreen({ navigation }: Props) {
  const { data, isLoading, isError, refetch } = useExpenses();

  if (isLoading) {
    return <LoadingState message="Loading expenses…" />;
  }
  if (isError || !data) {
    return <ErrorState message="Unable to load expenses." onRetry={refetch} />;
  }
  if (data.length === 0) {
    return (
      <EmptyState
        title="No expenses yet"
        message="Add your first expense to start tracking your spending."
        actionLabel="Add expense"
        onAction={() => navigation.navigate('ExpenseForm')}
      />
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.content}>
      {data.map((group) => (
        <View key={group.date}>
          <SectionHeader title={groupHeading(group.date)} />
          {group.expenses.map((expense, i) => (
            <View key={expense.id}>
              {i > 0 ? <Divider /> : null}
              <ExpenseRow
                expense={expense}
                onPress={() => navigation.navigate('ExpenseForm', { expenseId: expense.id })}
              />
            </View>
          ))}
        </View>
      ))}
    </ScrollView>
  );
}

function ExpenseRow({ expense, onPress }: { expense: Expense; onPress: () => void }) {
  return (
    <TouchableRipple
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={`Edit ${expense.categoryName ?? 'expense'}`}
    >
      <View style={styles.row}>
        <View style={styles.rowText}>
          <Text variant="bodyLarge">{expense.categoryName ?? 'Uncategorized'}</Text>
          <Text variant="bodySmall" style={styles.muted}>
            {[expense.description, expense.paymentMethodLabel].filter(Boolean).join(' · ') || '—'}
          </Text>
        </View>
        <Text variant="titleMedium">{formatCurrency(expense.amount, expense.currency)}</Text>
      </View>
    </TouchableRipple>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.md, paddingBottom: spacing.xl },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: spacing.sm,
    gap: spacing.md,
  },
  rowText: { flexShrink: 1 },
  muted: { color: palette.textSecondary },
});
