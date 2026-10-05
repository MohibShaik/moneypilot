import { zodResolver } from '@hookform/resolvers/zod';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { HelperText } from 'react-native-paper';

import { AppButton, AppInput, ErrorState, LoadingState } from '@/components';
import type { MoreStackParamList } from '@/navigation/types';
import { spacing } from '@/theme/theme';
import { editProfileSchema, type EditProfileFormValues } from './validation';
import { useProfile } from './useProfile';

type Props = NativeStackScreenProps<MoreStackParamList, 'EditProfile'>;

/**
 * Edit profile (HLD §15, §23). React Hook Form + Zod: display name required,
 * currency (3 uppercase), timezone (IANA). Email is shown read-only and is not
 * editable here. UI-first: submission is a placeholder (no API). Prefilled from
 * the current profile.
 */
export function EditProfileScreen({ navigation }: Props) {
  const { data, isLoading, isError, refetch } = useProfile();
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<EditProfileFormValues>({
    resolver: zodResolver(editProfileSchema),
    defaultValues: {
      displayName: data?.displayName ?? '',
      currency: data?.currency ?? '',
      timezone: data?.timezone ?? '',
    },
    values: data
      ? { displayName: data.displayName, currency: data.currency, timezone: data.timezone }
      : undefined,
    mode: 'onBlur',
  });

  if (isLoading) return <LoadingState message="Loading profile…" />;
  if (isError || !data) return <ErrorState message="Unable to load your profile." onRetry={refetch} />;

  const onSubmit = async (_values: EditProfileFormValues) => {
    setFormError(null);
    setSubmitting(true);
    try {
      // UI-first placeholder: no network call.
      navigation.goBack();
    } catch {
      setFormError('Unable to save your profile. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {formError ? (
          <HelperText type="error" visible>
            {formError}
          </HelperText>
        ) : null}

        <AppInput label="Email" value={data.email} disabled />

        <Controller
          control={control}
          name="displayName"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Display name"
              value={value}
              onChangeText={onChange}
              errorText={errors.displayName?.message}
              autoCapitalize="words"
            />
          )}
        />
        <Controller
          control={control}
          name="currency"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Currency"
              value={value}
              onChangeText={(t) => onChange(t.toUpperCase())}
              errorText={errors.currency?.message}
              autoCapitalize="characters"
            />
          )}
        />
        <Controller
          control={control}
          name="timezone"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Timezone"
              value={value}
              onChangeText={onChange}
              errorText={errors.timezone?.message}
              autoCapitalize="none"
            />
          )}
        />

        <View style={styles.submit}>
          <AppButton
            label="Save changes"
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
