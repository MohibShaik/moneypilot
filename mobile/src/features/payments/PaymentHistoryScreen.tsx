import { Ionicons } from '@expo/vector-icons';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Divider, Text, TouchableRipple } from 'react-native-paper';

import { EmptyState, ErrorState, LoadingState, SectionHeader } from '@/components';
import type { MoreStackParamList } from '@/navigation/types';
import { palette, spacing } from '@/theme/theme';
import { formatCurrency } from '@/utils/format';
import { paymentTypeIcon, paymentTypeLabel } from './paymentType';
import type { Payment } from './types';
import { usePayments } from './usePayments';

type Props = NativeStackScreenProps<MoreStackParamList, 'PaymentHistory'>;

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
 * Payment history (HLD §14): payments grouped by date, each clearly labeled by
 * type — Loan EMI, Credit Card Payment, or Other Payment. Payments are not
 * expenses. Loading/empty/error states (§22).
 */
export function PaymentHistoryScreen({ navigation }: Props) {
  const { data, isLoading, isError, refetch } = usePayments();

  if (isLoading) return <LoadingState message="Loading payments…" />;
  if (isError || !data) return <ErrorState message="Unable to load payments." onRetry={refetch} />;
  if (data.length === 0) {
    return <EmptyState title="No payments yet" message="Your payment history will appear here." />;
  }

  return (
    <ScrollView contentContainerStyle={styles.content}>
      {data.map((group) => (
        <View key={group.date}>
          <SectionHeader title={groupHeading(group.date)} />
          {group.payments.map((payment, i) => (
            <View key={payment.id}>
              {i > 0 ? <Divider /> : null}
              <PaymentRow
                payment={payment}
                onPress={() => navigation.navigate('PaymentDetails', { paymentId: payment.id })}
              />
            </View>
          ))}
        </View>
      ))}
    </ScrollView>
  );
}

function PaymentRow({ payment, onPress }: { payment: Payment; onPress: () => void }) {
  return (
    <TouchableRipple onPress={onPress} accessibilityRole="button">
      <View style={styles.row}>
        <Ionicons
          name={paymentTypeIcon(payment.paymentType)}
          size={22}
          color={palette.textSecondary}
          style={styles.icon}
        />
        <View style={styles.rowText}>
          <Text variant="bodyLarge">{paymentTypeLabel(payment.paymentType)}</Text>
          <Text variant="bodySmall" style={styles.muted}>
            {payment.label}
          </Text>
        </View>
        <Text variant="titleMedium">{formatCurrency(payment.amount, payment.currency)}</Text>
      </View>
    </TouchableRipple>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.md, paddingBottom: spacing.xl },
  row: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.sm, gap: spacing.sm },
  icon: { width: 24, textAlign: 'center' },
  rowText: { flexShrink: 1, flexGrow: 1 },
  muted: { color: palette.textSecondary },
});
