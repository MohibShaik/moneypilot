import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ErrorState, FinancialCard, LoadingState } from '@/components';
import type { MoreStackParamList } from '@/navigation/types';
import { palette, spacing } from '@/theme/theme';
import { formatCurrency, formatShortDate } from '@/utils/format';
import { paymentTypeLabel } from './paymentType';
import { usePayment } from './usePayments';

type Props = NativeStackScreenProps<MoreStackParamList, 'PaymentDetails'>;

/**
 * Payment details (HLD §14): shows the type, target, amount, method, and date
 * of a single payment. Loading/error states (§22).
 */
export function PaymentDetailsScreen({ route }: Props) {
  const { paymentId } = route.params;
  const { data, isLoading, isError, refetch } = usePayment(paymentId);

  if (isLoading) return <LoadingState message="Loading payment…" />;
  if (isError || !data) return <ErrorState message="Unable to load this payment." onRetry={refetch} />;

  const rows: Array<[string, string]> = [
    ['Type', paymentTypeLabel(data.paymentType)],
    ['Toward', data.label],
    ['Amount', formatCurrency(data.amount, data.currency)],
    ['Payment method', data.paymentMethodLabel ?? '—'],
    ['Date', formatShortDate(data.paidAt)],
  ];

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <FinancialCard title="Payment">
        {rows.map(([label, value]) => (
          <View key={label} style={styles.rowBetween}>
            <Text variant="bodyMedium" style={styles.muted}>
              {label}
            </Text>
            <Text variant="titleSmall">{value}</Text>
          </View>
        ))}
      </FinancialCard>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.md, gap: spacing.xs },
  rowBetween: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: spacing.xs,
    gap: spacing.md,
  },
  muted: { color: palette.textSecondary },
});
