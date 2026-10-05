import { StyleSheet, View } from 'react-native';
import { ActivityIndicator, Text } from 'react-native-paper';

import { spacing } from '@/theme/theme';

export type LoadingStateProps = {
  message?: string;
};

/**
 * Consistent loading indicator for server-backed screens (HLD §22).
 */
export function LoadingState({ message = 'Loading…' }: LoadingStateProps) {
  return (
    <View style={styles.container} accessibilityRole="progressbar">
      <ActivityIndicator animating accessibilityLabel={message} />
      <Text variant="bodyMedium" style={styles.message}>
        {message}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { alignItems: 'center', justifyContent: 'center', padding: spacing.lg, gap: spacing.sm },
  message: { textAlign: 'center' },
});
