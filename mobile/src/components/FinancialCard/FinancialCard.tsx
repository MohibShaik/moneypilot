import { StyleSheet, View } from 'react-native';
import { Card, Text } from 'react-native-paper';

import { palette, radius, spacing } from '@/theme/theme';

export type FinancialCardTone = 'default' | 'hero';

export type FinancialCardProps = {
  title: string;
  subtitle?: string;
  /** Optional trailing element in the header (e.g. a StatusBadge). */
  headerRight?: React.ReactNode;
  children?: React.ReactNode;
  onPress?: () => void;
  /**
   * Visual tone. `default` is a white surface card; `hero` is the dark charcoal
   * "balance" card used for a primary figure (e.g. total outstanding), with
   * light text on the charcoal surface.
   */
  tone?: FinancialCardTone;
};

/**
 * Container card for a financial entity summary — loan, credit card, etc.
 * (HLD §11, §12, §17). Presentation only; it renders values provided to it and
 * performs no financial calculation.
 */
export function FinancialCard({
  title,
  subtitle,
  headerRight,
  children,
  onPress,
  tone = 'default',
}: FinancialCardProps) {
  const isHero = tone === 'hero';
  return (
    <Card
      style={[styles.card, isHero && styles.heroCard]}
      onPress={onPress}
      mode={isHero ? 'contained' : 'elevated'}
    >
      <Card.Content>
        <View style={styles.header}>
          <View style={styles.headerText}>
            <Text variant="titleMedium" style={isHero ? styles.heroTitle : undefined}>
              {title}
            </Text>
            {subtitle ? (
              <Text
                variant="bodySmall"
                style={isHero ? styles.heroSubtitle : styles.subtitle}
              >
                {subtitle}
              </Text>
            ) : null}
          </View>
          {headerRight}
        </View>
        {children ? <View style={styles.body}>{children}</View> : null}
      </Card.Content>
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { borderRadius: radius.card, marginVertical: spacing.xs },
  heroCard: { backgroundColor: palette.primary },
  header: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between' },
  headerText: { flexShrink: 1 },
  subtitle: { color: palette.textSecondary },
  heroTitle: { color: palette.white },
  heroSubtitle: { color: palette.textDisabled },
  body: { marginTop: spacing.sm, gap: spacing.sm },
});
