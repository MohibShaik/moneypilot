import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Divider, Text } from 'react-native-paper';

import {
  AmountText,
  EmptyState,
  ErrorState,
  FinancialCard,
  LoadingState,
  SectionHeader,
  StatusBadge,
} from '@/components';
import type { CardsStackParamList } from '@/navigation/types';
import { palette, spacing } from '@/theme/theme';
import { formatCurrency, formatShortDate, maskCardLastFour } from '@/utils/format';
import type { CardDetail, CardStatement } from './types';
import { useCreditCard } from './useCreditCards';

type Props = NativeStackScreenProps<CardsStackParamList, 'CardDetails'>;

/**
 * Credit card details (HLD §12): card summary, current statement, and statement
 * history. Only masked last-four is shown. Loading/error states explicit (§22).
 */
export function CardDetailsScreen({ route }: Props) {
  const { cardId } = route.params;
  const { data, isLoading, isError, refetch } = useCreditCard(cardId);

  if (isLoading) {
    return <LoadingState message="Loading card…" />;
  }
  if (isError || !data) {
    return <ErrorState message="Unable to load this card." onRetry={refetch} />;
  }

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <CardSummarySection card={data} />

      <SectionHeader title="Current statement" />
      {data.currentStatement ? (
        <StatementCard statement={data.currentStatement} currency={data.currency} />
      ) : (
        <EmptyState title="No statement yet" message="No statement has been generated for this card." />
      )}

      <SectionHeader title="Statement history" />
      {data.statementHistory.length === 0 ? (
        <EmptyState title="No past statements" />
      ) : (
        data.statementHistory.map((s, i) => (
          <View key={s.id}>
            {i > 0 ? <Divider style={styles.divider} /> : null}
            <StatementCard statement={s} currency={data.currency} />
          </View>
        ))
      )}
    </ScrollView>
  );
}

function CardSummarySection({ card }: { card: CardDetail }) {
  return (
    <FinancialCard
      title={card.cardName}
      subtitle={`${card.bank} · ${maskCardLastFour(card.lastFour)}`}
      headerRight={<StatusBadge status={card.status} />}
    >
      <View style={styles.rowBetween}>
        <AmountText value={card.creditLimit} label="Credit limit" currency={card.currency} size="small" />
        {card.availableAmount != null ? (
          <AmountText
            value={card.availableAmount}
            label="Available"
            currency={card.currency}
            size="small"
            muted
          />
        ) : null}
      </View>
    </FinancialCard>
  );
}

function StatementCard({ statement, currency }: { statement: CardStatement; currency: string }) {
  const remaining = Math.max(statement.statementAmount - statement.paidAmount, 0);
  const rows: Array<[string, string]> = [
    ['Statement amount', formatCurrency(statement.statementAmount, currency)],
    ['Minimum due', formatCurrency(statement.minimumDue, currency)],
    ['Paid', formatCurrency(statement.paidAmount, currency)],
    ['Remaining', formatCurrency(remaining, currency)],
    ['Due date', formatShortDate(statement.dueDate)],
  ];
  return (
    <FinancialCard
      title={`Statement · ${formatShortDate(statement.statementDate)}`}
      headerRight={<StatusBadge status={statement.status} />}
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

const styles = StyleSheet.create({
  content: { padding: spacing.md, paddingBottom: spacing.xl, gap: spacing.xs },
  rowBetween: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: spacing.xs,
    gap: spacing.md,
  },
  muted: { color: palette.textSecondary },
  divider: { marginVertical: spacing.xs },
});
