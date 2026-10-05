import { AppInput } from '@/components/AppInput';

export type DateFieldProps = {
  label?: string;
  /** ISO date string (YYYY-MM-DD), controlled. */
  value: string;
  onChangeText: (text: string) => void;
  errorText?: string;
  disabled?: boolean;
  testID?: string;
};

/**
 * Date entry field (HLD §17). For UI TASK 2 this is a lightweight text field
 * accepting an ISO date (YYYY-MM-DD); a native date picker can be layered in a
 * later task without changing the field's value contract. Authoritative date
 * validation remains server-side; callers validate format with Zod.
 */
export function DateField({
  label = 'Date',
  value,
  onChangeText,
  errorText,
  disabled,
  testID,
}: DateFieldProps) {
  return (
    <AppInput
      label={label}
      value={value}
      onChangeText={onChangeText}
      errorText={errorText}
      disabled={disabled}
      placeholder="YYYY-MM-DD"
      keyboardType="default"
      autoCapitalize="none"
      testID={testID}
    />
  );
}
