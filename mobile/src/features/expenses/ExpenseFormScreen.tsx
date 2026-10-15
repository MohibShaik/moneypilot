import { zodResolver } from '@hookform/resolvers/zod';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { HelperText } from 'react-native-paper';

import {
  AppButton,
  AppInput,
  useToast,
  CurrencyInput,
  DateField,
  ErrorState,
  LoadingState,
  SelectField,
} from '@/components';
import type { ExpensesStackParamList } from '@/navigation/types';
import type { ExpenseDto, ExpenseInput } from '@/services/api/expenseApi';
import { spacing } from '@/theme/theme';
import { useCategories, useExpense, usePaymentMethods, useSaveExpense } from './useExpenses';
import { expenseFormSchema, type ExpenseFormValues } from './validation';

type Props = NativeStackScreenProps<ExpensesStackParamList, 'ExpenseForm'>;

const NONE_OPTION = { value: '', label: 'None' };

/**
 * Add / edit expense (HLD 13, 23). Edit mode loads the expense from
 * GET /expenses/{id} and renders the form only once it is available, so the
 * form defaults are correct from the first render. Save calls POST or PUT.
 */
export function ExpenseFormScreen({ route, navigation }: Props) {
  const expenseId = route.params?.expenseId;
  const existing = useExpense(expenseId);

  if (expenseId && existing.isLoading) {
    return <LoadingState message="Loading expense…" />;
  }
  if (expenseId && (existing.isError || !existing.data)) {
    return (
      <ErrorState
        message="Unable to load this expense."
        onRetry={() => {
          void existing.refetch();
        }}
      />
    );
  }

  return (
    <ExpenseForm
      expenseId={expenseId}
      existing={existing.data}
      onDone={() => navigation.goBack()}
    />
  );
}

type FormProps = {
  expenseId?: string;
  existing?: ExpenseDto;
  onDone: () => void;
};

function ExpenseForm({ expenseId, existing, onDone }: FormProps) {
  const isEdit = Boolean(expenseId);
  const categories = useCategories();
  const paymentMethods = usePaymentMethods();
  const categoryOptions = [
    NONE_OPTION,
    ...categories
      .filter((c) => c.categoryType === 'EXPENSE')
      .map((c) => ({ value: c.id, label: c.name })),
  ];
  const paymentMethodOptions = [
    NONE_OPTION,
    ...paymentMethods.map((p) => ({ value: p.id, label: p.label })),
  ];

  const { showSuccess, showError } = useToast();
  const save = useSaveExpense(expenseId);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    setError,
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

  const onSubmit = (values: ExpenseFormValues) => {
    setFormError(null);
    // Blank optional fields are omitted. On update the backend treats a missing
    // value as null, which clears the field.
    const body: ExpenseInput = {
      amount: Number(values.amount),
      expenseDate: values.expenseDate,
    };
    if (values.categoryId) body.categoryId = values.categoryId;
    if (values.paymentMethodId) body.paymentMethodId = values.paymentMethodId;
    if (values.description) body.description = values.description;
    if (values.notes) body.notes = values.notes;

    save.mutate(body, {
      onSuccess: () => {
        showSuccess(isEdit ? 'Expense updated' : 'Expense added');
        onDone();
      },
      onError: (err) => {
        if (err.fieldErrors) {
          for (const [field, message] of Object.entries(err.fieldErrors)) {
            if (field in values) {
              setError(field as keyof ExpenseFormValues, { type: 'server', message });
            }
          }
        }
        const msg = err.message ?? 'Unable to save the expense. Please try again.';
        setFormError(msg);
        showError(msg);
      },
    });
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
            <CurrencyInput
              label="Amount"
              value={value}
              onChangeText={onChange}
              errorText={errors.amount?.message}
            />
          )}
        />
        <Controller
          control={control}
          name="categoryId"
          render={({ field: { onChange, value } }) => (
            <SelectField
              label="Category (optional)"
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
              label="Payment method (optional)"
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
            <DateField
              label="Date"
              value={value}
              onChangeText={onChange}
              errorText={errors.expenseDate?.message}
            />
          )}
        />
        <Controller
          control={control}
          name="description"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Description (optional)"
              value={value}
              onChangeText={onChange}
              errorText={errors.description?.message}
              multiline
              numberOfLines={2}
            />
          )}
        />
        <Controller
          control={control}
          name="notes"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Notes (optional)"
              value={value}
              onChangeText={onChange}
              errorText={errors.notes?.message}
              multiline
              numberOfLines={4}
            />
          )}
        />

        <View style={styles.submit}>
          <AppButton
            label={isEdit ? 'Save changes' : 'Add expense'}
            variant="accent"
            onPress={handleSubmit(onSubmit)}
            loading={save.isPending}
            disabled={save.isPending}
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
