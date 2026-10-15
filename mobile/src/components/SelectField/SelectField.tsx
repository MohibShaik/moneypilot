import { useState } from 'react';
import { LayoutChangeEvent, Pressable, ScrollView, StyleSheet, View } from 'react-native';
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
 * Dropdown select built on Paper Menu + a read-only TextInput anchor
 * (HLD 17). Tapping anywhere on the field opens the menu, which is anchored
 * under the field and matches its width (measured on layout). Long lists
 * scroll within a capped height.
 */
export function SelectField<T extends string>({
  label,
  value,
  options,
  onChange,
  errorText,
  placeholder = 'Select...',
  disabled = false,
  testID,
}: SelectFieldProps<T>) {
  const [open, setOpen] = useState(false);
  const [width, setWidth] = useState(0);
  const hasError = Boolean(errorText);
  const selected = options.find((o) => o.value === value);

  const toggle = () => {
    if (!disabled) setOpen((v) => !v);
  };

  const onAnchorLayout = (e: LayoutChangeEvent) => {
    setWidth(e.nativeEvent.layout.width);
  };

  return (
    <View style={styles.container}>
      <View onLayout={onAnchorLayout}>
        <Menu
          visible={open}
          onDismiss={() => setOpen(false)}
          contentStyle={[styles.menuContent, width > 0 ? { width } : null]}
          anchor={
            <Pressable onPress={toggle} disabled={disabled} testID={testID}>
              <View pointerEvents="none">
                <TextInput
                  mode="outlined"
                  label={label}
                  value={selected?.label ?? ''}
                  placeholder={placeholder}
                  editable={false}
                  error={hasError}
                  disabled={disabled}
                  right={<TextInput.Icon icon={open ? 'menu-up' : 'menu-down'} />}
                />
              </View>
            </Pressable>
          }
        >
          <ScrollView style={styles.scroll} keyboardShouldPersistTaps="handled">
            {options.map((opt) => (
              <Menu.Item
                key={opt.value}
                title={opt.label}
                titleStyle={value === opt.value ? styles.selectedTitle : undefined}
                onPress={() => {
                  onChange(opt.value);
                  setOpen(false);
                }}
              />
            ))}
          </ScrollView>
        </Menu>
      </View>
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
  menuContent: { paddingVertical: 0 },
  scroll: { maxHeight: 280 },
  selectedTitle: { fontWeight: '700' },
});
