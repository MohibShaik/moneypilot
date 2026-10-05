import { zodResolver } from '@hookform/resolvers/zod';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { HelperText } from 'react-native-paper';

import { AppButton, AppInput, CurrencyInput, SelectField } from '@/components';
import type { CardsStackParamList } from '@/navigation/types';
import { spacing } from '@/theme/theme';
import { findMockCardDetail } from './mock/mockCards';
import { CARD_STATUS_OPTIONS, cardFormSchema, type CardFormValues } from './validation';

type Props = NativeStackScreenProps<CardsStackParamList, 'CardForm'>;

/**
 * Add / edit credit card form (HLD §12, §23). React Hook Form + Zod with
 * validation mirroring the backend V2 constraints. Only the last four digits
 * are captured — never full PAN, CVV, or PIN (§24). UI-first: submission is a
 * placeholder (no API call); edit mode pre-fills from mock detail.
 */
export function CardFormScreen({ route, navigation }: Props) {
  const cardId = route.params?.cardId;
  const existing = cardId ? findMockCardDetail(cardId) : undefined;
  const isEdit = Boolean(existing);

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
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

  const onSubmit = async (_values: CardFormValues) => {
    setFormError(null);
    setSubmitting(true);
    try {
      // UI-first placeholder: no network call.
      navigation.goBack();
    } catch {
      setFormError('Unable to save the card. Please try again.');
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
              label="Statement day (1–31)"
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
              label="Payment due day (1–31)"
              value={value}
              onChangeText={(t) => onChange(t.replace(/[^0-9]/g, '').slice(0, 2))}
              errorText={errors.paymentDueDay?.message}
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
              options={CARD_STATUS_OPTIONS}
              onChange={onChange}
              errorText={errors.status?.message}
            />
          )}
        />

        <View style={styles.submit}>
          <AppButton
            label={isEdit ? 'Save changes' : 'Add card'}
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
