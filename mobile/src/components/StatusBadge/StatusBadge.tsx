import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { radius, spacing, statusColors, type StatusKey } from '@/theme/theme';

export type StatusBadgeProps = {
  status: StatusKey;
  /** Optional override label; defaults to a humanized status. */
  label?: string;
};

function humanize(status: string): string {
  return status
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

/**
 * Status pill for financial states (HLD §11, §17). The status is always shown
 * as text in addition to color, so meaning never depends on color alone
 * (accessibility, §25).
 */
export function StatusBadge({ status, label }: StatusBadgeProps) {
  const color = statusColors[status];
  const text = label ?? humanize(status);
  return (
    <View style={[styles.badge, { borderColor: color }]} accessibilityRole="text">
      <View style={[styles.dot, { backgroundColor: color }]} />
      <Text variant="labelSmall" style={{ color }}>
        {text}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: spacing.xs,
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.sm,
    borderRadius: radius.pill,
    borderWidth: 1,
  },
  dot: { width: 8, height: 8, borderRadius: radius.pill },
});
