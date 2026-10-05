import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { AppButton } from '@/components/AppButton';
import { palette, spacing } from '@/theme/theme';

export type ErrorStateProps = {
  /** Generic, user-safe message. Never surface stack traces or DB errors (§22). */
  message?: string;
  onRetry?: () => void;
  retryLabel?: string;
};

/**
 * Consistent error state for server-backed screens (HLD §22). Messages are
 * generic and never expose internal/stack details.
 */
export function ErrorState({
  message = 'Something went wrong. Please try again.',
  onRetry,
  retryLabel = 'Retry',
}: ErrorStateProps) {
  return (
    <View style={styles.container} accessibilityRole="alert">
      <Text variant="titleMedium" style={styles.title}>
        {message}
      </Text>
      {onRetry ? (
        <View style={styles.action}>
          <AppButton label={retryLabel} onPress={onRetry} variant="secondary" />
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { alignItems: 'center', justifyContent: 'center', padding: spacing.lg, gap: spacing.sm },
  title: { textAlign: 'center', color: palette.textPrimary },
  action: { marginTop: spacing.sm },
});
