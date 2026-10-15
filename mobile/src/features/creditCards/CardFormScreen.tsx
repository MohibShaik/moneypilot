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
  ErrorState,
  LoadingState,
  SelectField,
  useToast,
} from '@/components';
import type { CardsStackParamList } from '@/navigation/types';
import type {
  CreateCreditCardInput,
  UpdateCreditCardInput,
} from '@/services/api/creditCardApi';
import type { CardDetail } from './types';
import { spacing } from '@/theme/theme';
import { useCreditCard, useSaveCreditCard } from './useCreditCards';
import { CARD_STATUS_OPTIONS, cardFormSchema, type CardFormValues } from './validation';

type Props = NativeStackScreenProps<CardsStackParamList, 'CardForm'>;

/**
 * Add / edit credit card (HLD 12, 23). Only the last four digits are captured,
 * never full PAN/CVV/PIN. lastFour is immutable, so on edit it renders disabled
 * and is not sent. Status is editable only on edit (create defaults to ACTIVE).
 */
export function CardFormScreen({ route, navigation }: Props) {
  const cardId = route.params?.cardId;
  const existing = useCreditCard(cardId ?? '');

  if (cardId && existing.isLoading) {
    return <LoadingState message="Loading card..." />;
  }
  if (cardId && (existing.isError || !existing.data)) {
    return (
      <ErrorState
        message="Unable to load this card."
        onRetry={() => {
          void existing.refetch();
        }}
      />
    );
  }

  return (
    <CardForm cardId={cardId} existing={existing.data ?? undefined} onDone={() => navigation.goBack()} />
  );
}

type FormProps = {
  cardId?: string;
  existing?: CardDetail;
  onDone: () => void;
};

function CardForm({ cardId, existing, onDone }: FormProps) {
  const isEdit = Boolean(cardId);
  const save = useSaveCreditCard(cardId);
  const { showSuccess, showError } = useToast();
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<CardFormValues>({
    resolver: zodResolver(cardFormSchema),
    defaultValues: {
      cardName: existing?.cardName ?? '',
      bank: existing?.bank ?? '',
      lastFour: existing?.lastFour ?? '',
      creditLimit: existing ? String(existing.creditLimit) : '',
      statementDay: '',
      paymentDueDay: '',
      status: existing?.status ?? 'ACTIVE',
    },
    mode: 'onBlur',
  });

  const onSubmit = (values: CardFormValues) => {
    setFormError(null);
    const body: CreateCreditCardInput | UpdateCreditCardInput = isEdit
      ? {
          cardName: values.cardName,
          bank: values.bank,
          creditLimit: Number(values.creditLimit),
          statementDay: Number(values.statementDay),
          paymentDueDay: Number(values.paymentDueDay),
          status: values.status,
        }
      : {
          cardName: values.cardName,
          bank: values.bank,
          lastFour: values.lastFour,
          creditLimit: Number(values.creditLimit),
          statementDay: Number(values.statementDay),
          paymentDueDay: Number(values.paymentDueDay),
        };

    save.mutate(body, {
      onSuccess: () => {
        showSuccess(isEdit ? 'Card updated' : 'Card added');
        onDone();
      },
      onError: (err) => {
        if (err.fieldErrors) {
          for (const [field, message] of Object.entries(err.fieldErrors)) {
            if (field in values) {
              setError(field as keyof CardFormValues, { type: 'server', message });
            }
          }
        }
        const msg = err.message ?? 'Unable to save the card. Please try again.';
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
          name="cardName"
          render={({ field: { onChange, value } }) => (
            <AppInput label="Card name" value={value} onChangeText={onChange} errorText={errors.cardName?.message} />
          )}
        />
        <Controller
          control={control}
          name="bank"
          render={({ field: { onChange, value } }) => (
            <AppInput label="Bank" value={value} onChangeText={onChange} errorText={errors.bank?.message} />
          )}
        />
        <Controller
          control={control}
          name="lastFour"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Last 4 digits"
              value={value}
              onChangeText={(t) => onChange(t.replace(/[^0-9]/g, '').slice(0, 4))}
              errorText={errors.lastFour?.message}
              keyboardType="numeric"
              placeholder="4521"
              disabled={isEdit}
            />
          )}
        />
        <Controller
          control={control}
          name="creditLimit"
          render={({ field: { onChange, value } }) => (
            <CurrencyInput label="Credit limit" value={value} onChangeText={onChange} errorText={errors.creditLimit?.message} />
          )}
        />
        <Controller
          control={control}
          name="statementDay"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Statement day (1-31)"
              value={value}
              onChangeText={(t) => onChange(t.replace(/[^0-9]/g, '').slice(0, 2))}
              errorText={errors.statementDay?.message}
              keyboardType="numeric"
            />
          )}
        />
        <Controller
          control={control}
          name="paymentDueDay"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Payment due day (1-31)"
              value={value}
              onChangeText={(t) => onChange(t.replace(/[^0-9]/g, '').slice(0, 2))}
              errorText={errors.paymentDueDay?.message}
              keyboardType="numeric"
            />
          )}
        />
        {isEdit ? (
          <Controller
            control={control}
            name="status"
            render={({ field: { onChange, value } }) => (
              <SelectField label="Status" value={value} options={CARD_STATUS_OPTIONS} onChange={onChange} errorText={errors.status?.message} />
            )}
          />
        ) : null}

        <View style={styles.submit}>
          <AppButton
            label={isEdit ? 'Save changes' : 'Add card'}
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
