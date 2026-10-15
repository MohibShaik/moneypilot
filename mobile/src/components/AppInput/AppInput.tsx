import { StyleSheet, View } from 'react-native';
import { HelperText, TextInput } from 'react-native-paper';

import { spacing } from '@/theme/theme';

export type AppInputProps = {
  label?: string;
  value?: string;
  onChangeText?: (text: string) => void;
  placeholder?: string;
  /** When set, the field renders in an error state and shows this message. */
  errorText?: string;
  secureTextEntry?: boolean;
  keyboardType?: 'default' | 'email-address' | 'numeric' | 'decimal-pad';
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
  disabled?: boolean;
  /** Render a taller multi-line text area (e.g. for notes). */
  multiline?: boolean;
  /** Visible rows when multiline (default 4). */
  numberOfLines?: number;
  /** Optional trailing icon affordance (e.g. a password visibility toggle). */
  right?: React.ReactNode;
  testID?: string;
};

/**
 * Standard text input (HLD 17). Wraps Paper TextInput with an attached error
 * HelperText so field-level validation messages render consistently and are
 * announced to screen readers (accessibility, 25). Supports an optional
 * multi-line mode for longer free text such as notes.
 */
export function AppInput({
  label,
  value,
  onChangeText,
  placeholder,
  errorText,
  secureTextEntry,
  keyboardType = 'default',
  autoCapitalize = 'sentences',
  disabled = false,
  multiline = false,
  numberOfLines = 4,
  right,
  testID,
}: AppInputProps) {
  const hasError = Boolean(errorText);
  return (
    <View style={styles.container}>
      <TextInput
        mode="outlined"
        label={label}
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        error={hasError}
        secureTextEntry={secureTextEntry}
        keyboardType={keyboardType}
        autoCapitalize={autoCapitalize}
        disabled={disabled}
        multiline={multiline}
        numberOfLines={multiline ? numberOfLines : undefined}
        style={multiline ? { minHeight: 24 * numberOfLines } : undefined}
        right={right}
        testID={testID}
      />
      {hasError ? (
        <HelperText type="error" visible={hasError}>
          {errorText}
        </HelperText>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { marginBottom: spacing.xs },
});
