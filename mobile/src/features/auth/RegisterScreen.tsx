import { zodResolver } from '@hookform/resolvers/zod';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { HelperText, Snackbar, Text, TextInput } from 'react-native-paper';

import { AppButton } from '@/components/AppButton';
import { AppInput } from '@/components/AppInput';
import type { AuthStackParamList } from '@/navigation/types';
import { palette, spacing } from '@/theme/theme';
import { registerSchema, type RegisterFormValues } from './validation';

type Props = NativeStackScreenProps<AuthStackParamList, 'Register'>;

/**
 * Registration screen (HLD §9).
 *
 * Collects display name, email, password, confirm password, and optional
 * currency/timezone, validated client-side with React Hook Form + Zod. The
 * rules mirror the backend (name 1–100, email ≤254, password 8–128, currency
 * 3 uppercase, IANA timezone) with confirm-password enforced only on the client.
 *
 * UI-first: submission is a placeholder — it does not call the API and issues no
 * token (registration never issues a JWT, HLD §9/§10). On success the user is
 * returned to Login to sign in. Real `POST /api/v1/auth/register` wiring happens
 * during API integration without changing this screen's structure.
 */
export function RegisterScreen({ navigation }: Props) {
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [registered, setRegistered] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      displayName: '',
      email: '',
      password: '',
      confirmPassword: '',
      currency: '',
      timezone: '',
    },
    mode: 'onBlur',
  });

  const onSubmit = async (_values: RegisterFormValues) => {
    setFormError(null);
    setSubmitting(true);
    try {
      // UI-first placeholder: no network call, no token. Registration returns a
      // profile on the backend; the user then logs in to obtain a token.
      setRegistered(true);
    } catch {
      setFormError('Unable to create your account. Please try again.');
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
        <View style={styles.header}>
          <Text variant="headlineMedium">Create your account</Text>
          <Text variant="bodyMedium" style={styles.subtitle}>
            Start tracking loans, cards, and expenses in one place
          </Text>
        </View>

        {formError ? (
          <HelperText type="error" visible style={styles.formError}>
            {formError}
          </HelperText>
        ) : null}

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
              testID="register-name"
            />
          )}
        />

        <Controller
          control={control}
          name="email"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Email"
              placeholder="you@example.com"
              value={value}
              onChangeText={onChange}
              errorText={errors.email?.message}
              keyboardType="email-address"
              autoCapitalize="none"
              testID="register-email"
            />
          )}
        />

        <Controller
          control={control}
          name="password"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Password"
              value={value}
              onChangeText={onChange}
              errorText={errors.password?.message}
              secureTextEntry={!showPassword}
              autoCapitalize="none"
              testID="register-password"
              right={
                <TextInput.Icon
                  icon={showPassword ? 'eye-off' : 'eye'}
                  onPress={() => setShowPassword((v) => !v)}
                  accessibilityLabel={showPassword ? 'Hide password' : 'Show password'}
                />
              }
            />
          )}
        />

        <Controller
          control={control}
          name="confirmPassword"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Confirm password"
              value={value}
              onChangeText={onChange}
              errorText={errors.confirmPassword?.message}
              secureTextEntry={!showConfirm}
              autoCapitalize="none"
              testID="register-confirm"
              right={
                <TextInput.Icon
                  icon={showConfirm ? 'eye-off' : 'eye'}
                  onPress={() => setShowConfirm((v) => !v)}
                  accessibilityLabel={showConfirm ? 'Hide password' : 'Show password'}
                />
              }
            />
          )}
        />

        <Controller
          control={control}
          name="currency"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Currency (optional)"
              placeholder="INR"
              value={value}
              onChangeText={(t) => onChange(t.toUpperCase())}
              errorText={errors.currency?.message}
              autoCapitalize="characters"
              testID="register-currency"
            />
          )}
        />

        <Controller
          control={control}
          name="timezone"
          render={({ field: { onChange, value } }) => (
            <AppInput
              label="Timezone (optional)"
              placeholder="Asia/Kolkata"
              value={value}
              onChangeText={onChange}
              errorText={errors.timezone?.message}
              autoCapitalize="none"
              testID="register-timezone"
            />
          )}
        />

        <View style={styles.submit}>
          <AppButton
            label="Create account"
            variant="accent"
            onPress={handleSubmit(onSubmit)}
            loading={submitting}
            disabled={submitting}
          />
        </View>

        <View style={styles.footer}>
          <AppButton
            label="Already have an account? Log in"
            variant="text"
            onPress={() => navigation.navigate('Login')}
          />
        </View>
      </ScrollView>

      <Snackbar
        visible={registered}
        onDismiss={() => setRegistered(false)}
        duration={2500}
        action={{ label: 'Log in', onPress: () => navigation.navigate('Login') }}
      >
        Account created. Please log in.
      </Snackbar>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  content: { flexGrow: 1, justifyContent: 'center', padding: spacing.lg, gap: spacing.sm },
  header: { alignItems: 'center', marginBottom: spacing.md },
  subtitle: { color: palette.textSecondary, marginTop: spacing.xs, textAlign: 'center' },
  formError: { textAlign: 'center' },
  submit: { marginTop: spacing.sm },
  footer: { alignItems: 'center', marginTop: spacing.sm },
});
