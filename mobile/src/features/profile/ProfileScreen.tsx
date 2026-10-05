import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { AppButton, ConfirmDialog, ErrorState, FinancialCard, LoadingState } from '@/components';
import type { MoreStackParamList } from '@/navigation/types';
import { useAuthStore } from '@/store/authStore';
import { palette, spacing } from '@/theme/theme';
import { useProfile } from './useProfile';

type Props = NativeStackScreenProps<MoreStackParamList, 'Profile'>;

/**
 * Profile screen (HLD §15): shows name, email, currency, and timezone, with
 * actions to edit the profile, open settings, and log out. Never shows a
 * password hash or sensitive auth data (§15, §24). Loading/error states (§22).
 */
export function ProfileScreen({ navigation }: Props) {
  const { data, isLoading, isError, refetch } = useProfile();
  const signOut = useAuthStore((s) => s.signOut);
  const [confirmLogout, setConfirmLogout] = useState(false);

  if (isLoading) return <LoadingState message="Loading profile…" />;
  if (isError || !data) return <ErrorState message="Unable to load your profile." onRetry={refetch} />;

  const rows: Array<[string, string]> = [
    ['Name', data.displayName],
    ['Email', data.email],
    ['Currency', data.currency],
    ['Timezone', data.timezone],
  ];

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <FinancialCard title={data.displayName} subtitle={data.email}>
        {rows.map(([label, value]) => (
          <View key={label} style={styles.rowBetween}>
            <Text variant="bodyMedium" style={styles.muted}>
              {label}
            </Text>
            <Text variant="titleSmall">{value}</Text>
          </View>
        ))}
      </FinancialCard>

      <View style={styles.actions}>
        <AppButton label="Edit profile" variant="accent" onPress={() => navigation.navigate('EditProfile')} />
        <AppButton label="Settings" variant="secondary" onPress={() => navigation.navigate('Settings')} />
        <AppButton label="Log out" variant="text" onPress={() => setConfirmLogout(true)} />
      </View>

      <ConfirmDialog
        visible={confirmLogout}
        title="Log out?"
        message="You'll need to sign in again to access your account."
        confirmLabel="Log out"
        destructive
        onConfirm={() => {
          setConfirmLogout(false);
          signOut();
        }}
        onCancel={() => setConfirmLogout(false)}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.md, gap: spacing.sm },
  rowBetween: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: spacing.xs,
    gap: spacing.md,
  },
  muted: { color: palette.textSecondary },
  actions: { gap: spacing.sm, marginTop: spacing.sm },
});
