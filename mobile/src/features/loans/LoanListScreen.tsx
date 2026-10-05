import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { AmountText, EmptyState, ErrorState, FinancialCard, LoadingState, StatusBadge } from '@/components';
import type { LoansStackParamList } from '@/navigation/types';
import { palette, spacing } from '@/theme/theme';
import { formatShortDate } from '@/utils/format';
import type { LoanSummary } from './types';
import { useLoans } from './useLoans';

type Props = NativeStackScreenProps<LoansStackParamList, 'LoanList'>;

function loanTypeLabel(type: LoanSummary['loanType']): string {
  return type.charAt(0) + type.slice(1).toLowerCase();
}

/**
 * Loan list (HLD §11). Each card shows name, lender, type, outstanding
 * principal, EMI, next due date, and status. Renders loading/empty/error states
 * (HLD §22). Status is shown with label + color, never color alone (§25).
 */
export function LoanListScreen({ navigation }: Props) {
  const { data, isLoading, isError, refetch } = useLoans();

  if (isLoading) {
    return <LoadingState message="Loading loans…" />;
  }
  if (isError || !data) {
    return <ErrorState message="Unable to load loans." onRetry={refetch} />;
  }
  if (data.length === 0) {
    return (
      <EmptyState
        title="No loans yet"
        message="Add your first loan to start tracking your EMI schedule."
      />
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.content}>
      {data.map((loan) => (
        <FinancialCard
          key={loan.id}
          title={loan.name}
          subtitle={[loan.lender, loanTypeLabel(loan.loanType)].filter(Boolean).join(' · ')}
          headerRight={<StatusBadge status={loan.status} />}
          onPress={() => navigation.navigate('LoanDetails', { loanId: loan.id })}
        >
          <View style={styles.rowBetween}>
            <AmountText
              value={loan.outstandingPrincipal}
              label="Outstanding"
              currency={loan.currency}
              size="small"
            />
            <AmountText value={loan.emiAmount} label="EMI" currency={loan.currency} size="small" />
          </View>
          {loan.nextDueDate ? (
            <Text variant="bodySmall" style={styles.muted}>
              Next EMI · {formatShortDate(loan.nextDueDate)}
            </Text>
          ) : null}
        </FinancialCard>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.md, gap: spacing.xs },
  rowBetween: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: spacing.md,
  },
  muted: { color: palette.textSecondary },
});
