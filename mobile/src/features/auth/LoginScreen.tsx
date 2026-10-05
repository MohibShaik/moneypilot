import { zodResolver } from '@hookform/resolvers/zod';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { HelperText, Text, TextInput } from 'react-native-paper';

import { AppButton } from '@/components/AppButton';
import { AppInput } from '@/components/AppInput';
import type { AuthStackParamList } from '@/navigation/types';
import { useAuthStore } from '@/store/authStore';
import { palette, spacing } from '@/theme/theme';
import { loginSchema, type LoginFormValues } from './validation';

type Props = NativeStackScreenProps<AuthStackParamList, 'Login'>;

/**
 * Login screen (HLD §8).
 *
 * UI-first: validates input with React Hook Form + Zod, then performs a
 * placeholder sign-in that flips the session to authenticated. It does NOT mint
 * or store a fake JWT (HLD §8). Real authentication against
 * `POST /api/v1/auth/login` is wired during API integration; this screen's
 * structure (form, submit, error handling) stays the same.
 */
export function LoginScreen({ navigation }: Props) {
  const signIn = useAuthStore((s) => s.signIn);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
    mode: 'onBlur',
  });

  const onSubmit = async (values: LoginFormValues) => {
    setFormError(null);
    setSubmitting(true);
    try {
      // UI-first placeholder: no network call, no token. Marks the session
      // active so the root navigator switches to the main app.
      signIn(values.email);
    } catch {
      // When wired to the API, a failed login surfaces a generic message here.
      setFormError('Unable to sign in. Please try again.');
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
          <Text variant="headlineMedium">MoneyPilot</Text>
          <Text variant="bodyMedium" style={styles.subtitle}>
            Sign in to your financial command center
          </Text>
        </View>

        {formError ? (
          <HelperText type="error" visible style={styles.formError}>
            {formError}
          </HelperText>
        ) : null}

        <Controller
          control={control}
          name="email"
          render={({ field: { onChange, onBlur, value } }) => (
            <AppInput
              label="Email"
              placeholder="you@example.com"
              value={value}
              onChangeText={onChange}
              errorText={errors.email?.message}
              keyboardType="email-address"
              autoCapitalize="none"
              testID="login-email"
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
              testID="login-password"
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

        <View style={styles.submit}>
          <AppButton
            label="Login"
            onPress={handleSubmit(onSubmit)}
            loading={submitting}
            disabled={submitting}
          />
        </View>

        <View style={styles.footer}>
          <AppButton
            label="Create an account"
            variant="text"
            onPress={() => navigation.navigate('Register')}
          />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  content: { flexGrow: 1, justifyContent: 'center', padding: spacing.lg, gap: spacing.sm },
  header: { alignItems: 'center', marginBottom: spacing.lg },
  subtitle: { color: palette.textSecondary, marginTop: spacing.xs, textAlign: 'center' },
  formError: { textAlign: 'center' },
  submit: { marginTop: spacing.sm },
  footer: { alignItems: 'center', marginTop: spacing.sm },
});
