import { StyleSheet } from 'react-native';
import { Button } from 'react-native-paper';

import { dimensions, palette } from '@/theme/theme';

export type AppButtonVariant = 'primary' | 'secondary' | 'accent' | 'text';

export type AppButtonProps = {
  label: string;
  onPress: () => void;
  variant?: AppButtonVariant;
  loading?: boolean;
  disabled?: boolean;
  icon?: string;
  /** Accessibility label; defaults to `label`. */
  accessibilityLabel?: string;
};

const MODE: Record<AppButtonVariant, 'contained' | 'outlined' | 'text'> = {
  primary: 'contained',
  secondary: 'outlined',
  accent: 'contained',
  text: 'text',
};

/**
 * Standard app button (HLD §17). Wraps Paper's Button with a consistent API,
 * a minimum touch target (accessibility, §25), and loading/disabled handling.
 */
export function AppButton({
  label,
  onPress,
  variant = 'primary',
  loading = false,
  disabled = false,
  icon,
  accessibilityLabel,
}: AppButtonProps) {
  return (
    <Button
      mode={MODE[variant]}
      onPress={onPress}
      loading={loading}
      disabled={disabled || loading}
      icon={icon}
      // Accent renders as a filled indigo button for quick actions; other
      // variants fall back to the theme's primary (navy).
      buttonColor={variant === 'accent' ? palette.accent : undefined}
      accessibilityLabel={accessibilityLabel ?? label}
      style={styles.button}
      contentStyle={styles.content}
    >
      {label}
    </Button>
  );
}

const styles = StyleSheet.create({
  button: { justifyContent: 'center' },
  content: { minHeight: dimensions.controlHeight },
});
