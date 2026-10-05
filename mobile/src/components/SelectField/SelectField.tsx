import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { HelperText, Menu, TextInput } from 'react-native-paper';

import { spacing } from '@/theme/theme';

export type SelectOption<T extends string> = {
  label: string;
  value: T;
};

export type SelectFieldProps<T extends string> = {
  label?: string;
  value: T | '';
  options: ReadonlyArray<SelectOption<T>>;
  onChange: (value: T) => void;
  errorText?: string;
  placeholder?: string;
  disabled?: boolean;
  testID?: string;
};

/**
 * Dropdown select built on Paper's Menu + a read-only TextInput anchor
 * (HLD §17). Used for enum fields (loan type, status, etc.). Keeps the same
 * visual language as AppInput and surfaces validation errors consistently.
 */
export function SelectField<T extends string>({
  label,
  value,
  options,
  onChange,
  errorText,
  placeholder = 'Select…',
  disabled = false,
  testID,
}: SelectFieldProps<T>) {
  const [open, setOpen] = useState(false);
  const hasError = Boolean(errorText);
  const selected = options.find((o) => o.value === value);

  return (
    <View style={styles.container}>
      <Menu
        visible={open}
        onDismiss={() => setOpen(false)}
        anchor={
          <TextInput
            mode="outlined"
            label={label}
            value={selected?.label ?? ''}
            placeholder={placeholder}
            editable={false}
            error={hasError}
            disabled={disabled}
            testID={testID}
            right={<TextInput.Icon icon="menu-down" onPress={() => !disabled && setOpen(true)} />}
            onPressIn={() => !disabled && setOpen(true)}
          />
        }
      >
        {options.map((opt) => (
          <Menu.Item
            key={opt.value}
            title={opt.label}
            onPress={() => {
              onChange(opt.value);
              setOpen(false);
            }}
          />
        ))}
      </Menu>
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
