import { zodResolver } from '@hookform/resolvers/zod';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { HelperText } from 'react-native-paper';

import { AppButton, AppInput, CurrencyInput, DateField, SelectField } from '@/components';
import type { LoansStackParamList } from '@/navigation/types';
import { spacing } from '@/theme/theme';
import { findMockLoanDetail } from './mock/mockLoans';
import {
  LOAN_STATUS_OPTIONS,
  LOAN_TYPE_OPTIONS,
  loanFormSchema,
  type LoanFormValues,
} from './validation';

type Props = NativeStackScreenProps<LoansStackParamList, 'LoanForm'>;

/**
 * Add / edit loan form (HLD §11, §23). React Hook Form + Zod with validation
 * mirroring the backend V2 constraints. UI-first: submission is a placeholder —
 * no API call. In edit mode it pre-fills from mock detail. Real
 * create/update wiring (`/api/v1/loans`) is added during API integration
 * without changing this screen's structure.
 */
export function LoanFormScreen({ route, navigation }: Props) {
  const loanId = route.params?.loanId;
  const existing = loanId ? findMockLoanDetail(loanId) : undefined;
  const isEdit = Boolean(existing);

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<LoanFormValues>({
    resolver: zodResolver(loanFormSchema),
    defaultValues: {
      name: existing?.name ?? '',
      lender: existing?.lender ?? '',
      loanType: existing?.loanType ?? 'HOME',
      principalAmount: existing ? String(existing.principalAmount) : '',
      outstandingPrincipal: existing ? String(existing.outstandingPrincipal) : '',
      emiAmount: existing ? String(existing.emiAmount) : '',
      interestRate: existing ? String(existing.interestRate) : '',
      startDate: existing?.startDate ?? '',
      endDate: existing?.endDate ?? '',
      tenureMonths: existing ? String(existing.tenureMonths) : '',
      status: existing?.status ?? 'ACTIVE',
    },
    mode: 'onBlur',
  });

  const onSubmit = async (_values: LoanFormValues) => {
    setFormError(null);
    setSubmitting(true);
    try {
      // UI-first placeholder: no network call. Return to the previous screen.
      navigation.goBack();
    } catch {
      setFormError('Unable to save the loan. Please try again.');
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
          name="name"
          render={({ field: { onChange, value } }) => (
            <AppInput label="Loan name" value={value} onChangeText={onChange} errorText={errors.name?.message} />
          )}
        />
        <Controller
          control={control}
          name="lender"
          render={({ field: { onChange, value } }) => (
            <AppInput label="Lender (optional)" value={value} onChangeText={onChange} errorText={errors.lender?.message} />
          )}
        />
        <Controller
          control={control}
          name="loanType"
          render={({ field: { onChange, value } }) => (
            <SelectField
              label="Loan type"
              value={value}
              options={LOAN_TYPE_OPTIONS}
              onChange={onChange}
              errorText={errors.loanType?.message}
            />
          )}
        />
        <Controller
          control={control}
          name="principalAmount"
          render={({ field: { onChange, value } }) => (
            <CurrencyInput label="Principal amount" value={value} onChangeText={onChange} errorText={errors.principalAmount?.message} />
          )}
        />
        <Controller
          control={control}
          name="outstandingPrincipal"
          render={({ field: { onChange, value } }) => (
            <CurrencyInput label="Outstanding principal" value={value} onChangeText={onChange} errorText={errors.outstandingPrincipal?.message} />
          )}
        />
        <Controller
          control={control}
          name="emiAmount"
          render={({ field: { onChange, value } }) => (
            <CurrencyInput label="EMI amount" value={value} onChangeText={onChange} errorText={errors.emiAmount?.message} />
          )}
        />
        <Controller
          control={control}
          name="interestRate"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Interest rate (%)"
              value={value}
              onChangeText={onChange}
              errorText={errors.interestRate?.message}
              keyboardType="decimal-pad"
            />
          )}
        />
        <Controller
          control={control}
          name="startDate"
          render={({ field: { onChange, value } }) => (
            <DateField label="Start date" value={value} onChangeText={onChange} errorText={errors.startDate?.message} />
          )}
        />
        <Controller
          control={control}
          name="endDate"
          render={({ field: { onChange, value } }) => (
            <DateField label="End date (optional)" value={value} onChangeText={onChange} errorText={errors.endDate?.message} />
          )}
        />
        <Controller
          control={control}
          name="tenureMonths"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Tenure (months)"
              value={value}
              onChangeText={onChange}
              errorText={errors.tenureMonths?.message}
              keyboardType="numeric"
            />
          )}
        />
        <Controller
          control={control}
          name="status"
          render={({ field: { onChange, value } }) => (
            <SelectField
              label="Status"
              value={value}
              options={LOAN_STATUS_OPTIONS}
              onChange={onChange}
              errorText={errors.status?.message}
            />
          )}
        />

        <View style={styles.submit}>
          <AppButton
            label={isEdit ? 'Save changes' : 'Add loan'}
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
