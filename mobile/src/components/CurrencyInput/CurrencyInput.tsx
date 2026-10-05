import { AppInput } from '@/components/AppInput';

export type CurrencyInputProps = {
  label?: string;
  /** Current raw text value (controlled). */
  value: string;
  onChangeText: (text: string) => void;
  errorText?: string;
  disabled?: boolean;
  placeholder?: string;
  testID?: string;
};

/**
 * Numeric amount input (HLD §17). Restricts input to a decimal number with up to
 * two fraction digits and uses the decimal keypad. It does not compute or
 * validate authoritative financial state — that remains server-side (§4.1);
 * callers parse the string (e.g. with Zod) for submission.
 */
export function CurrencyInput({
  label = 'Amount',
  value,
  onChangeText,
  errorText,
  disabled,
  placeholder = '0.00',
  testID,
}: CurrencyInputProps) {
  const handleChange = (text: string) => {
    // Allow only digits and a single decimal point with <= 2 fraction digits.
    const cleaned = text.replace(/[^0-9.]/g, '');
    const match = cleaned.match(/^(\d*)(\.\d{0,2})?/);
    onChangeText(match ? match[0] : '');
  };

  return (
    <AppInput
      label={label}
      value={value}
      onChangeText={handleChange}
      errorText={errorText}
      disabled={disabled}
      placeholder={placeholder}
      keyboardType="decimal-pad"
      testID={testID}
    />
  );
}
