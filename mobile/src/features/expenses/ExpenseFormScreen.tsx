import { zodResolver } from '@hookform/resolvers/zod';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { HelperText } from 'react-native-paper';

import { AppButton, AppInput, CurrencyInput, DateField, SelectField } from '@/components';
import type { ExpensesStackParamList } from '@/navigation/types';
import { spacing } from '@/theme/theme';
import { findMockExpense } from './mock/mockExpenses';
import { expenseFormSchema, type ExpenseFormValues } from './validation';
import { useCategories, usePaymentMethods } from './useExpenses';

type Props = NativeStackScreenProps<ExpensesStackParamList, 'ExpenseForm'>;

/**
 * Add / edit expense form (HLD §13, §23). React Hook Form + Zod: amount > 0,
 * valid category and payment method, valid date; description/notes optional.
 * UI-first: submission is a placeholder (no API). Edit mode pre-fills from mock.
 */
export function ExpenseFormScreen({ route, navigation }: Props) {
  const expenseId = route.params?.expenseId;
  const existing = expenseId ? findMockExpense(expenseId) : undefined;
  const isEdit = Boolean(existing);

  const categories = useCategories();
  const paymentMethods = usePaymentMethods();
  const categoryOptions = categories
    .filter((c) => c.categoryType === 'EXPENSE')
    .map((c) => ({ value: c.id, label: c.name }));
  const paymentMethodOptions = paymentMethods.map((p) => ({ value: p.id, label: p.label }));

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<ExpenseFormValues>({
    resolver: zodResolver(expenseFormSchema),
    defaultValues: {
      amount: existing ? String(existing.amount) : '',
      categoryId: existing?.categoryId ?? '',
      paymentMethodId: existing?.paymentMethodId ?? '',
      expenseDate: existing?.expenseDate ?? '',
      description: existing?.description ?? '',
      notes: existing?.notes ?? '',
    },
    mode: 'onBlur',
  });

  const onSubmit = async (_values: ExpenseFormValues) => {
    setFormError(null);
    setSubmitting(true);
    try {
      navigation.goBack();
    } catch {
      setFormError('Unable to save the expense. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {formError ? (
          <HelperText type="error" visible>
            {formError}
          </HelperText>
        ) : null}

        <Controller
          control={control}
          name="amount"
          render={({ field: { onChange, value } }) => (
            <CurrencyInput label="Amount" value={value} onChangeText={onChange} errorText={errors.amount?.message} />
          )}
        />
        <Controller
          control={control}
          name="categoryId"
          render={({ field: { onChange, value } }) => (
            <SelectField
              label="Category"
              value={value}
              options={categoryOptions}
              onChange={onChange}
              errorText={errors.categoryId?.message}
            />
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
          name="expenseDate"
          render={({ field: { onChange, value } }) => (
            <DateField label="Date" value={value} onChangeText={onChange} errorText={errors.expenseDate?.message} />
          )}
        />
        <Controller
          control={control}
          name="description"
          render={({ field: { onChange, value } }) => (
            <AppInput label="Description (optional)" value={value} onChangeText={onChange} errorText={errors.description?.message} />
          )}
        />
        <Controller
          control={control}
          name="notes"
          render={({ field: { onChange, value } }) => (
            <AppInput label="Notes (optional)" value={value} onChangeText={onChange} errorText={errors.notes?.message} />
          )}
        />

        <View style={styles.submit}>
          <AppButton
            label={isEdit ? 'Save changes' : 'Add expense'}
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
  submit: { marginTop: spacing.sm },
});
