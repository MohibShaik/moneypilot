import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { HelperText, Text } from 'react-native-paper';

import {
  AmountText,
  AppButton,
  CurrencyInput,
  DateField,
  ErrorState,
  FinancialCard,
  LoadingState,
  SelectField,
} from '@/components';
import { usePaymentMethods } from '@/features/expenses';
import { palette, spacing } from '@/theme/theme';
import { makePaymentSchema, type PaymentFormValues } from './validation';
import { usePayableTarget } from './usePayments';

export type PaymentFormProps = {
  targetId: string;
  /** Called after a successful (placeholder) submit. */
  onDone: () => void;
};

/**
 * Shared payment form for a loan EMI or a card statement (HLD §14). It loads the
 * payable target to show the authoritative total / already-paid / remaining
 * amounts (backend-computed), then collects a payment amount (partial allowed,
 * capped at remaining), method, and date. UI-first: submission is a placeholder
 * (no API). The backend determines the authoritative remaining and settlement.
 */
export function PaymentForm({ targetId, onDone }: PaymentFormProps) {
  const { data: target, isLoading, isError, refetch } = usePayableTarget(targetId);
  const paymentMethods = usePaymentMethods();
  const paymentMethodOptions = paymentMethods.map((p) => ({ value: p.id, label: p.label }));

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<PaymentFormValues>({
    // Remaining defaults to a large cap until the target loads; the schema is
    // rebuilt with the real remaining once available (see resolver below).
    resolver: zodResolver(makePaymentSchema(target?.remaining ?? Number.MAX_SAFE_INTEGER)),
    defaultValues: { amount: '', paymentMethodId: '', paymentDate: '' },
    mode: 'onBlur',
  });

  if (isLoading) return <LoadingState message="Loading…" />;
  if (isError || !target) return <ErrorState message="Unable to load payment details." onRetry={refetch} />;

  const onSubmit = async (_values: PaymentFormValues) => {
    setFormError(null);
    setSubmitting(true);
    try {
      // UI-first placeholder: no network call. The backend will validate the
      // amount against the authoritative remaining and record the payment.
      onDone();
    } catch {
      setFormError('Unable to record the payment. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <FinancialCard title={target.title}>
          <View style={styles.rowBetween}>
            <AmountText value={target.totalAmount} label="Total" currency={target.currency} size="small" />
            <AmountText value={target.alreadyPaid} label="Paid" currency={target.currency} size="small" muted />
            <AmountText value={target.remaining} label="Remaining" currency={target.currency} size="small" />
          </View>
          <Text variant="bodySmall" style={styles.muted}>
            Partial payments are supported. The remaining amount is confirmed by the server.
          </Text>
        </FinancialCard>

        {formError ? (
          <HelperText type="error" visible>
            {formError}
          </HelperText>
        ) : null}

        <Controller
          control={control}
          name="amount"
          render={({ field: { onChange, value } }) => (
            <CurrencyInput label="Payment amount" value={value} onChangeText={onChange} errorText={errors.amount?.message} />
          )}
        />
        <Controller
          control={control}
          name="paymentMethodId"
          render={({ field: { onChange, value } }) => (
            <SelectField
              label="Payment method"
              value={value}
              options={paymentMethodOptions}
              onChange={onChange}
              errorText={errors.paymentMethodId?.message}
            />
          )}
        />
        <Controller
          control={control}
          name="paymentDate"
          render={({ field: { onChange, value } }) => (
            <DateField label="Payment date" value={value} onChangeText={onChange} errorText={errors.paymentDate?.message} />
          )}
        />

        <View style={styles.submit}>
          <AppButton
            label="Make payment"
            variant="accent"
            onPress={handleSubmit(onSubmit)}
            loading={submitting}
            disabled={submitting}
          />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  content: { padding: spacing.md, paddingBottom: spacing.xl, gap: spacing.xs },
  rowBetween: { flexDirection: 'row', justifyContent: 'space-between', gap: spacing.sm },
  muted: { color: palette.textSecondary },
  submit: { marginTop: spacing.sm },
});
