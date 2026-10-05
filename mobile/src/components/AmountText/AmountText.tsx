import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { palette, spacing } from '@/theme/theme';
import { formatCurrency } from '@/utils/format';

export type AmountTextProps = {
  /** Pre-computed amount from the backend/mock (never calculated here). */
  value: number;
  /** Optional label rendered above the amount (e.g. "Outstanding"). */
  label?: string;
  currency?: string;
  size?: 'small' | 'medium' | 'large';
  /** Visually de-emphasize (e.g. secondary amounts). */
  muted?: boolean;
  /** Render with light text, for use on a dark (hero) surface. */
  onDark?: boolean;
};

const VARIANT = {
  small: 'titleSmall',
  medium: 'titleMedium',
  large: 'headlineSmall',
} as const;

/**
 * Displays a monetary value with consistent formatting (HLD §17). Formatting is
 * presentation-only; the value must already be authoritative.
 */
export function AmountText({
  value,
  label,
  currency = 'INR',
  size = 'medium',
  muted = false,
  onDark = false,
}: AmountTextProps) {
  const formatted = formatCurrency(value, currency);
  const labelStyle = onDark ? styles.labelOnDark : styles.label;
  const amountStyle = onDark
    ? styles.onDark
    : muted
      ? styles.muted
      : undefined;
  return (
    <View>
      {label ? (
        <Text variant="labelMedium" style={labelStyle}>
          {label}
        </Text>
      ) : null}
      <Text
        variant={VARIANT[size]}
        style={amountStyle}
        accessibilityLabel={label ? `${label}: ${formatted}` : formatted}
      >
        {formatted}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  label: { color: palette.textSecondary, marginBottom: spacing.xs },
  labelOnDark: { color: palette.textDisabled, marginBottom: spacing.xs },
  muted: { color: palette.textSecondary },
  onDark: { color: palette.white },
});
