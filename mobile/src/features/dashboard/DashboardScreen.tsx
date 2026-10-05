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
import { palette, spacing } from '@/theme/theme';
import { formatCurrency, formatShortDate, greetingForHour } from '@/utils/format';
import type { DashboardData, UpcomingPayment } from './types';
import { useDashboard } from './useDashboard';

/**
 * Dashboard / Home screen (HLD §10): greeting, financial overview, upcoming
 * payments, monthly spending, and loan/credit-card summaries. Data comes from
 * the `useDashboard` abstraction (mock now, API later) and the screen renders
 * explicit loading/empty/error states (HLD §22).
 */
export function DashboardScreen() {
  const { data, isLoading, isError, refetch } = useDashboard();

  if (isLoading) {
    return <LoadingState message="Loading your dashboard…" />;
  }

  if (isError || !data) {
    return <ErrorState message="Unable to load your dashboard." onRetry={refetch} />;
  }

  const hasAnyData =
    data.overview.totalOutstanding > 0 ||
    data.upcomingPayments.length > 0 ||
    data.loanSummary.loanCount > 0 ||
    data.creditCardSummary.cardCount > 0;

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <Greeting name={data.userName} />

      <SectionHeader title="Financial overview" />
      <FinancialCard title="Total outstanding" tone="hero">
        <AmountText
          value={data.overview.totalOutstanding}
          currency={data.overview.currency}
          size="large"
          onDark
        />
      </FinancialCard>

      {!hasAnyData ? (
        <EmptyState
          title="Nothing to show yet"
          message="Add a loan, credit card, or expense to see your overview here."
        />
      ) : (
        <>
          <UpcomingPayments payments={data.upcomingPayments} currency={data.overview.currency} />
          <MonthlySpending data={data} />
          <Summaries data={data} />
        </>
      )}
    </ScrollView>
  );
}

function Greeting({ name }: { name: string }) {
  return (
    <View style={styles.greeting}>
      <Text variant="headlineSmall" accessibilityRole="header">
        {greetingForHour()}, {name} 👋
      </Text>
    </View>
  );
}

function UpcomingPayments({
  payments,
  currency,
}: {
  payments: UpcomingPayment[];
  currency: string;
}) {
  return (
    <>
      <SectionHeader title="Upcoming payments" />
      {payments.length === 0 ? (
        <EmptyState title="No upcoming payments" message="You're all caught up." />
      ) : (
        <FinancialCard title="Due soon">
          {payments.map((p, i) => (
            <View key={p.id}>
              {i > 0 ? <Divider style={styles.divider} /> : null}
              <View style={styles.rowBetween}>
                <View style={styles.flexShrink}>
                  <Text variant="bodyLarge">{p.label}</Text>
                  <Text variant="bodySmall" style={styles.muted}>
                    Due {formatShortDate(p.dueDate)}
                  </Text>
                </View>
                <Text variant="titleMedium">{formatCurrency(p.amount, currency)}</Text>
              </View>
            </View>
          ))}
        </FinancialCard>
      )}
    </>
  );
}

function MonthlySpending({ data }: { data: DashboardData }) {
  const { monthlySpending, overview } = data;
  const rows: Array<[string, number]> = [
    ['Expenses', monthlySpending.expenses],
    ['Loan payments', monthlySpending.loanPayments],
    ['Card payments', monthlySpending.cardPayments],
  ];
  return (
    <>
      <SectionHeader title="This month" />
      <FinancialCard title="Summary">
        {rows.map(([label, value], i) => (
          <View key={label}>
            {i > 0 ? <Divider style={styles.divider} /> : null}
            <View style={styles.rowBetween}>
              <Text variant="bodyMedium" style={styles.muted}>
                {label}
              </Text>
              <Text variant="titleSmall">{formatCurrency(value, overview.currency)}</Text>
            </View>
          </View>
        ))}
      </FinancialCard>
    </>
  );
}

function Summaries({ data }: { data: DashboardData }) {
  const { loanSummary, creditCardSummary, overview } = data;
  return (
    <>
      <SectionHeader title="Loans" />
      <FinancialCard
        title={`${loanSummary.loanCount} loan${loanSummary.loanCount === 1 ? '' : 's'}`}
        headerRight={<StatusBadge status="ACTIVE" />}
      >
        <AmountText
          value={loanSummary.totalOutstanding}
          label="Outstanding"
          currency={overview.currency}
        />
      </FinancialCard>

      <SectionHeader title="Credit cards" />
      <FinancialCard
        title={`${creditCardSummary.cardCount} card${creditCardSummary.cardCount === 1 ? '' : 's'}`}
      >
        <View style={styles.rowBetween}>
          <AmountText
            value={creditCardSummary.totalOutstanding}
            label="Outstanding"
            currency={overview.currency}
            size="small"
          />
          <AmountText
            value={creditCardSummary.totalAvailable}
            label="Available"
            currency={overview.currency}
            size="small"
            muted
          />
        </View>
      </FinancialCard>
    </>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.md, paddingBottom: spacing.xl, gap: spacing.xs },
  greeting: { marginBottom: spacing.sm },
  rowBetween: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: spacing.xs,
    gap: spacing.sm,
  },
  flexShrink: { flexShrink: 1 },
  muted: { color: palette.textSecondary },
  divider: { marginVertical: spacing.xs },
});
