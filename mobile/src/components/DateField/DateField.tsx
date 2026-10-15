import DateTimePicker from '@react-native-community/datetimepicker';
import { useState } from 'react';
import { Platform, Pressable, StyleSheet, View } from 'react-native';
import { HelperText, Text, TextInput, useTheme } from 'react-native-paper';

import { spacing } from '@/theme/theme';

export type DateFieldProps = {
  label?: string;
  /** ISO date string (YYYY-MM-DD), controlled. */
  value: string;
  onChangeText: (text: string) => void;
  errorText?: string;
  disabled?: boolean;
  testID?: string;
};

/** Formats a Date as a local YYYY-MM-DD (no timezone drift). */
function toIso(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/** Parses a YYYY-MM-DD string into a local Date, or today if blank/invalid. */
function fromIso(s: string): Date {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s);
  if (!m) return new Date();
  return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
}

/**
 * Date entry field (HLD 17). On web it renders the browser native date input
 * (input type=date). On native it opens the platform DateTimePicker from a
 * tappable field. Both speak the same ISO YYYY-MM-DD value contract.
 */
export function DateField({
  label = 'Date',
  value,
  onChangeText,
  errorText,
  disabled,
  testID,
}: DateFieldProps) {
  const theme = useTheme();
  const hasError = Boolean(errorText);
  const [show, setShow] = useState(false);

  if (Platform.OS === 'web') {
    return (
      <View style={styles.container}>
        {label ? (
          <Text variant="bodySmall" style={styles.label}>
            {label}
          </Text>
        ) : null}
        <input
          type="date"
          value={value}
          disabled={disabled}
          onChange={(ev) => onChangeText((ev.target as unknown as { value: string }).value)}
          style={{
            height: 48,
            borderRadius: 4,
            border: `1px solid ${hasError ? theme.colors.error : theme.colors.outline}`,
            padding: `0 ${spacing.sm}px`,
            fontSize: 16,
            color: theme.colors.onSurface,
            backgroundColor: theme.colors.background,
            fontFamily: 'inherit',
            width: '100%',
            boxSizing: 'border-box',
          }}
        />
        {hasError ? (
          <HelperText type="error" visible={hasError}>
            {errorText}
          </HelperText>
        ) : null}
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Pressable onPress={() => !disabled && setShow(true)} testID={testID}>
        <View pointerEvents="none">
          <TextInput
            mode="outlined"
            label={label}
            value={value}
            placeholder="YYYY-MM-DD"
            editable={false}
            error={hasError}
            disabled={disabled}
            right={<TextInput.Icon icon="calendar" />}
          />
        </View>
      </Pressable>
      {show ? (
        <DateTimePicker
          value={fromIso(value)}
          mode="date"
          display={Platform.OS === 'ios' ? 'spinner' : 'default'}
          onChange={(event, selected) => {
            setShow(false);
            if (event.type === 'set' && selected) {
              onChangeText(toIso(selected));
            }
          }}
        />
      ) : null}
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
  label: { marginBottom: spacing.xs / 2 },
});
