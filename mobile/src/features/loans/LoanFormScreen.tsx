import { zodResolver } from '@hookform/resolvers/zod';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { HelperText } from 'react-native-paper';

import {
  AppButton,
  AppInput,
  CurrencyInput,
  DateField,
  ErrorState,
  LoadingState,
  SelectField,
  useToast,
} from '@/components';
import type { LoansStackParamList } from '@/navigation/types';
import type { CreateLoanInput, UpdateLoanInput } from '@/services/api/loanApi';
import { spacing } from '@/theme/theme';
import { useLoan, useSaveLoan } from './useLoans';
import {
  LOAN_STATUS_OPTIONS,
  LOAN_TYPE_OPTIONS,
  loanFormSchema,
  type LoanFormValues,
} from './validation';

type Props = NativeStackScreenProps<LoansStackParamList, 'LoanForm'>;

/**
 * Add / edit loan (HLD 11, 23). On create the backend derives outstanding
 * principal, end date and the EMI schedule, so those are not sent. On edit only
 * name, lender and status are mutable (financial terms are immutable in V1), so
 * the financial inputs render disabled. Edit mode loads the loan first.
 */
export function LoanFormScreen({ route, navigation }: Props) {
  const loanId = route.params?.loanId;
  const existing = useLoan(loanId ?? '');

  if (loanId && existing.isLoading) {
    return <LoadingState message="Loading loan..." />;
  }
  if (loanId && (existing.isError || !existing.data)) {
    return (
      <ErrorState
        message="Unable to load this loan."
        onRetry={() => {
          void existing.refetch();
        }}
      />
    );
  }

  return (
    <LoanForm loanId={loanId} existing={existing.data ?? undefined} onDone={() => navigation.goBack()} />
  );
}

type FormProps = {
  loanId?: string;
  existing?: {
    name: string;
    lender: string | null;
    loanType: LoanFormValues['loanType'];
    principalAmount: number;
    outstandingPrincipal: number;
    emiAmount: number;
    interestRate: number;
    startDate: string;
    endDate: string | null;
    tenureMonths: number;
    status: LoanFormValues['status'];
  };
  onDone: () => void;
};

function LoanForm({ loanId, existing, onDone }: FormProps) {
  const isEdit = Boolean(loanId);
  const save = useSaveLoan(loanId);
  const { showSuccess, showError } = useToast();
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    setError,
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

  const onSubmit = (values: LoanFormValues) => {
    setFormError(null);
    const body: CreateLoanInput | UpdateLoanInput = isEdit
      ? { name: values.name, lender: values.lender || undefined, status: values.status }
      : {
          name: values.name,
          lender: values.lender || undefined,
          loanType: values.loanType,
          principalAmount: Number(values.principalAmount),
          interestRate: Number(values.interestRate),
          startDate: values.startDate,
          tenureMonths: Number(values.tenureMonths),
          emiAmount: values.emiAmount ? Number(values.emiAmount) : undefined,
        };

    save.mutate(body, {
      onSuccess: () => {
        showSuccess(isEdit ? 'Loan updated' : 'Loan added');
        onDone();
      },
      onError: (err) => {
        if (err.fieldErrors) {
          for (const [field, message] of Object.entries(err.fieldErrors)) {
            if (field in values) {
              setError(field as keyof LoanFormValues, { type: 'server', message });
            }
          }
        }
        const msg = err.message ?? 'Unable to save the loan. Please try again.';
        setFormError(msg);
        showError(msg);
      },
    });
  };

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
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
            <SelectField label="Loan type" value={value} options={LOAN_TYPE_OPTIONS} onChange={onChange} errorText={errors.loanType?.message} disabled={isEdit} />
          )}
        />
        <Controller
          control={control}
          name="principalAmount"
          render={({ field: { onChange, value } }) => (
            <CurrencyInput label="Principal amount" value={value} onChangeText={onChange} errorText={errors.principalAmount?.message} disabled={isEdit} />
          )}
        />
        <Controller
          control={control}
          name="interestRate"
          render={({ field: { onChange, value } }) => (
            <AppInput label="Interest rate (%)" value={value} onChangeText={onChange} errorText={errors.interestRate?.message} keyboardType="decimal-pad" disabled={isEdit} />
          )}
        />
        <Controller
          control={control}
          name="emiAmount"
          render={({ field: { onChange, value } }) => (
            <CurrencyInput label="EMI amount (optional)" value={value ?? ''} onChangeText={onChange} errorText={errors.emiAmount?.message} disabled={isEdit} />
          )}
        />
        <Controller
          control={control}
          name="startDate"
          render={({ field: { onChange, value } }) => (
            <DateField label="Start date" value={value} onChangeText={onChange} errorText={errors.startDate?.message} disabled={isEdit} />
          )}
        />
        <Controller
          control={control}
          name="tenureMonths"
          render={({ field: { onChange, value } }) => (
            <AppInput label="Tenure (months)" value={value} onChangeText={onChange} errorText={errors.tenureMonths?.message} keyboardType="numeric" disabled={isEdit} />
          )}
        />
        {isEdit ? (
          <Controller
            control={control}
            name="status"
            render={({ field: { onChange, value } }) => (
              <SelectField label="Status" value={value} options={LOAN_STATUS_OPTIONS} onChange={onChange} errorText={errors.status?.message} />
            )}
          />
        ) : null}

        <View style={styles.submit}>
          <AppButton
            label={isEdit ? 'Save changes' : 'Add loan'}
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
