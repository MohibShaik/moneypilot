import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { AppButton } from '@/components/AppButton';
import { palette, spacing } from '@/theme/theme';

export type EmptyStateProps = {
  title: string;
  message?: string;
  /** Optional primary action (e.g. "Add Loan"). */
  actionLabel?: string;
  onAction?: () => void;
};

/**
 * Consistent empty state for lists/screens with no data (HLD §22).
 */
export function EmptyState({ title, message, actionLabel, onAction }: EmptyStateProps) {
  return (
    <View style={styles.container}>
      <Text variant="titleMedium" style={styles.title}>
        {title}
      </Text>
      {message ? (
        <Text variant="bodyMedium" style={styles.message}>
          {message}
        </Text>
      ) : null}
      {actionLabel && onAction ? (
        <View style={styles.action}>
          <AppButton label={actionLabel} onPress={onAction} />
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { alignItems: 'center', justifyContent: 'center', padding: spacing.lg, gap: spacing.sm },
  title: { textAlign: 'center', color: palette.textPrimary },
  message: { textAlign: 'center', color: palette.textSecondary },
  action: { marginTop: spacing.sm },
});
