import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { AmountText, EmptyState, ErrorState, FinancialCard, LoadingState, StatusBadge } from '@/components';
import type { CardsStackParamList } from '@/navigation/types';
import { palette, spacing } from '@/theme/theme';
import { formatShortDate, maskCardLastFour } from '@/utils/format';
import { useCreditCards } from './useCreditCards';

type Props = NativeStackScreenProps<CardsStackParamList, 'CardList'>;

/**
 * Credit card list (HLD §12). Each card shows name, bank, masked last four,
 * credit limit, available amount (when supplied), statement due date, and
 * status. Never shows full card number, CVV, or PIN (§12, §24). Renders
 * loading/empty/error states (§22); status via label + color (§25).
 */
export function CardListScreen({ navigation }: Props) {
  const { data, isLoading, isError, refetch } = useCreditCards();

  if (isLoading) {
    return <LoadingState message="Loading cards…" />;
  }
  if (isError || !data) {
    return <ErrorState message="Unable to load cards." onRetry={refetch} />;
  }
  if (data.length === 0) {
    return (
      <EmptyState
        title="No credit cards yet"
        message="Add a card to track statements and payment due dates."
      />
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.content}>
      {data.map((card) => (
        <FinancialCard
          key={card.id}
          title={card.cardName}
          subtitle={`${card.bank} · ${maskCardLastFour(card.lastFour)}`}
          headerRight={<StatusBadge status={card.status} />}
          onPress={() => navigation.navigate('CardDetails', { cardId: card.id })}
        >
          <View style={styles.rowBetween}>
            <AmountText value={card.creditLimit} label="Limit" currency={card.currency} size="small" />
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
          {card.dueDate ? (
            <Text variant="bodySmall" style={styles.muted}>
              Due · {formatShortDate(card.dueDate)}
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
