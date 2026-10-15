import { zodResolver } from '@hookform/resolvers/zod';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { HelperText, Snackbar, Text, TextInput } from 'react-native-paper';

import { AppButton } from '@/components/AppButton';
import { AppInput } from '@/components/AppInput';
import { useRegister } from '@/hooks/useRegister';
import type { AuthStackParamList } from '@/navigation/types';
import { palette, spacing } from '@/theme/theme';
import { registerSchema, type RegisterFormValues } from './validation';

type Props = NativeStackScreenProps<AuthStackParamList, 'Register'>;

/**
 * Registration screen (HLD §9).
 *
 * Collects display name, email, password, confirm password, and optional
 * currency/timezone, validated client-side with React Hook Form + Zod.
 * Calls POST /api/v1/auth/register via useRegister. Registration returns a
 * profile and issues no token, so on success we show a confirmation and route
 * the user to Login. Backend field errors are mapped onto the matching inputs.
 */
export function RegisterScreen({ navigation }: Props) {
  const registerMutation = useRegister();
  const [formError, setFormError] = useState<string | null>(null);
  const [registered, setRegistered] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const {
    control,
    handleSubmit,
    setError,
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

  const onSubmit = (values: RegisterFormValues) => {
    setFormError(null);
    registerMutation.mutate(
      {
        displayName: values.displayName,
        email: values.email,
        password: values.password,
        currency: values.currency,
        timezone: values.timezone,
      },
      {
        onSuccess: () => setRegistered(true),
        onError: (err) => {
          if (err.fieldErrors) {
            for (const [field, message] of Object.entries(err.fieldErrors)) {
              if (field in values) {
                setError(field as keyof RegisterFormValues, { type: 'server', message });
              }
            }
          }
          setFormError(err.message ?? 'Unable to create your account. Please try again.');
        },
      },
    );
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
            loading={registerMutation.isPending}
            disabled={registerMutation.isPending}
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
