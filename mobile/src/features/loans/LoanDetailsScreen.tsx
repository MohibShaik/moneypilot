import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Divider, Text } from 'react-native-paper';

import {
  AmountText,
  ErrorState,
  FinancialCard,
  LoadingState,
  SectionHeader,
  StatusBadge,
} from '@/components';
import type { LoansStackParamList } from '@/navigation/types';
import { palette, spacing } from '@/theme/theme';
import { formatCurrency, formatShortDate } from '@/utils/format';
import type { LoanDetail, LoanEmi } from './types';
import { useLoan } from './useLoans';

type Props = NativeStackScreenProps<LoansStackParamList, 'LoanDetails'>;

/**
 * Loan details (HLD §11): loan summary plus the EMI schedule. Loading/error
 * states are explicit (§22); EMI status uses label + color (§25). All values
 * are backend-computed and only displayed here (§4.1).
 */
export function LoanDetailsScreen({ route }: Props) {
  const { loanId } = route.params;
  const { data, isLoading, isError, refetch } = useLoan(loanId);

  if (isLoading) {
    return <LoadingState message="Loading loan…" />;
  }
  if (isError || !data) {
    return <ErrorState message="Unable to load this loan." onRetry={refetch} />;
  }

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <LoanSummarySection loan={data} />
      <SectionHeader title="EMI schedule" />
      {data.emis.map((emi, i) => (
        <View key={emi.id}>
          {i > 0 ? <Divider style={styles.divider} /> : null}
          <EmiRow emi={emi} currency={data.currency} />
        </View>
      ))}
    </ScrollView>
  );
}

function LoanSummarySection({ loan }: { loan: LoanDetail }) {
  const rows: Array<[string, string]> = [
    ['Principal', formatCurrency(loan.principalAmount, loan.currency)],
    ['Outstanding', formatCurrency(loan.outstandingPrincipal, loan.currency)],
    ['EMI', formatCurrency(loan.emiAmount, loan.currency)],
    ['Interest rate', `${loan.interestRate}%`],
    ['Start date', formatShortDate(loan.startDate)],
    ['End date', loan.endDate ? formatShortDate(loan.endDate) : '—'],
    ['Tenure', `${loan.tenureMonths} months`],
  ];
  return (
    <FinancialCard
      title={loan.name}
      subtitle={loan.lender ?? undefined}
      headerRight={<StatusBadge status={loan.status} />}
    >
      {rows.map(([label, value]) => (
        <View key={label} style={styles.rowBetween}>
          <Text variant="bodyMedium" style={styles.muted}>
            {label}
          </Text>
          <Text variant="titleSmall">{value}</Text>
        </View>
      ))}
    </FinancialCard>
  );
}

function EmiRow({ emi, currency }: { emi: LoanEmi; currency: string }) {
  const remaining = Math.max(emi.emiAmount - emi.paidAmount, 0);
  return (
    <View style={styles.emiRow}>
      <View style={styles.emiHeader}>
        <Text variant="titleSmall">
          #{emi.installmentNo} · {formatShortDate(emi.dueDate)}
        </Text>
        <StatusBadge status={emi.status} />
      </View>
      <View style={styles.rowBetween}>
        <AmountText value={emi.emiAmount} label="EMI" currency={currency} size="small" />
        <AmountText value={remaining} label="Remaining" currency={currency} size="small" muted />
      </View>
      <Text variant="bodySmall" style={styles.muted}>
        Principal {formatCurrency(emi.principalComponent, currency)} · Interest{' '}
        {formatCurrency(emi.interestComponent, currency)} · Paid{' '}
        {formatCurrency(emi.paidAmount, currency)}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.md, paddingBottom: spacing.xl, gap: spacing.xs },
  rowBetween: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: spacing.xs,
    gap: spacing.md,
  },
  emiRow: { paddingVertical: spacing.xs, gap: spacing.xs },
  emiHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.sm,
  },
  muted: { color: palette.textSecondary },
  divider: { marginVertical: spacing.xs },
});
