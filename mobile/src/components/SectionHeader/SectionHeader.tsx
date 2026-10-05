import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { palette, spacing } from '@/theme/theme';

export type SectionHeaderProps = {
  title: string;
  /** Optional trailing action (e.g. a "See all" button). */
  action?: React.ReactNode;
};

/**
 * Section heading used to group content on a screen (HLD §17).
 */
export function SectionHeader({ title, action }: SectionHeaderProps) {
  return (
    <View style={styles.container}>
      <Text variant="titleMedium" accessibilityRole="header" style={styles.title}>
        {title}
      </Text>
      {action}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.md,
    marginBottom: spacing.sm,
  },
  title: { color: palette.textPrimary },
});
