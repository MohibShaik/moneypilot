import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Divider, HelperText, List, Text } from 'react-native-paper';

import { AppButton, AppInput, EmptyState, ErrorState, LoadingState, SelectField } from '@/components';
import {
  createPaymentMethod,
  listPaymentMethods,
  type PaymentMethodTypeDto,
} from '@/services/api/paymentMethodApi';
import type { ApiError } from '@/types/api';
import { spacing } from '@/theme/theme';

// CREDIT_CARD is omitted: it requires a creditCardId (a later capability).
const TYPE_OPTIONS: ReadonlyArray<{ value: PaymentMethodTypeDto; label: string }> = [
  { value: 'CASH', label: 'Cash' },
  { value: 'UPI', label: 'UPI' },
  { value: 'DEBIT_CARD', label: 'Debit card' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer' },
  { value: 'OTHER', label: 'Other' },
];

/**
 * Payment methods management (More tab). Lists the user payment methods and
 * lets them add one (type + label). Payment methods are user-scoped on the
 * backend (user_id NOT NULL), so unlike categories they cannot be seeded and
 * must be created here. Adding one invalidates the shared ['payment-methods']
 * cache so the expense form picks it up immediately.
 */
export function PaymentMethodsScreen() {
  const queryClient = useQueryClient();
  const list = useQuery({ queryKey: ['payment-methods'], queryFn: listPaymentMethods });

  const [type, setType] = useState<PaymentMethodTypeDto | ''>('');
  const [label, setLabel] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const save = useMutation<unknown, ApiError, { type: PaymentMethodTypeDto; label: string }>({
    mutationFn: createPaymentMethod,
    onSuccess: async () => {
      setType('');
      setLabel('');
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: ['payment-methods'] });
    },
    onError: (err) => setFormError(err.message ?? 'Unable to add the payment method.'),
  });

  const onAdd = () => {
    setFormError(null);
    if (!type) {
      setFormError('Select a type.');
      return;
    }
    if (label.trim() === '') {
      setFormError('Enter a label.');
      return;
    }
    save.mutate({ type, label: label.trim() });
  };

  if (list.isLoading) {
    return <LoadingState message="Loading payment methods…" />;
  }
  if (list.isError) {
    return (
      <ErrorState
        message="Unable to load payment methods."
        onRetry={() => {
          void list.refetch();
        }}
      />
    );
  }

  const methods = list.data ?? [];

  return (
    <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      <Text variant="titleMedium" style={styles.heading}>
        Add a payment method
      </Text>
      <SelectField
        label="Type"
        value={type}
        options={TYPE_OPTIONS}
        onChange={(v) => setType(v)}
      />
      <AppInput label="Label" placeholder="e.g. HDFC Debit" value={label} onChangeText={setLabel} />
      {formError ? (
        <HelperText type="error" visible>
          {formError}
        </HelperText>
      ) : null}
      <View style={styles.addBtn}>
        <AppButton
          label="Add payment method"
          variant="accent"
          onPress={onAdd}
          loading={save.isPending}
          disabled={save.isPending}
        />
      </View>

      <Text variant="titleMedium" style={styles.heading}>
        Your payment methods
      </Text>
      {methods.length === 0 ? (
        <EmptyState title="No payment methods yet" message="Add one above to use it on expenses and payments." />
      ) : (
        methods.map((m, i) => (
          <View key={m.id}>
            {i > 0 ? <Divider /> : null}
            <List.Item title={m.label} description={m.type} />
          </View>
        ))
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.md, paddingBottom: spacing.xl, gap: spacing.xs },
  heading: { marginTop: spacing.md, marginBottom: spacing.xs },
  addBtn: { marginTop: spacing.sm },
});
