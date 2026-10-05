import { ScrollView, StyleSheet, View } from 'react-native';
import { Divider, List, Switch, Text } from 'react-native-paper';

import { AppButton, SectionHeader } from '@/components';
import { useAuthStore } from '@/store/authStore';
import { useUiPrefsStore } from '@/store/uiPrefsStore';
import { palette, spacing } from '@/theme/theme';
import { useProfile } from './useProfile';

/**
 * Settings screen (HLD §15): shows currency and timezone, toggles for theme
 * (dark mode) and notifications (client UI preferences, HLD §18), and logout.
 * No sensitive authentication data is shown (§15, §24).
 */
export function SettingsScreen() {
  const { data } = useProfile();
  const signOut = useAuthStore((s) => s.signOut);
  const themePreference = useUiPrefsStore((s) => s.themePreference);
  const setThemePreference = useUiPrefsStore((s) => s.setThemePreference);
  const notificationsEnabled = useUiPrefsStore((s) => s.notificationsEnabled);
  const setNotificationsEnabled = useUiPrefsStore((s) => s.setNotificationsEnabled);

  const darkMode = themePreference === 'dark';

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <SectionHeader title="Preferences" />
      <List.Section>
        <List.Item
          title="Currency"
          description={data?.currency ?? '—'}
          left={(p) => <List.Icon {...p} icon="currency-inr" />}
        />
        <Divider />
        <List.Item
          title="Timezone"
          description={data?.timezone ?? '—'}
          left={(p) => <List.Icon {...p} icon="earth" />}
        />
        <Divider />
        <List.Item
          title="Dark mode"
          left={(p) => <List.Icon {...p} icon="theme-light-dark" />}
          right={() => (
            <Switch
              value={darkMode}
              onValueChange={(v) => setThemePreference(v ? 'dark' : 'light')}
              accessibilityLabel="Toggle dark mode"
            />
          )}
        />
        <Divider />
        <List.Item
          title="Notifications"
          left={(p) => <List.Icon {...p} icon="bell-outline" />}
          right={() => (
            <Switch
              value={notificationsEnabled}
              onValueChange={setNotificationsEnabled}
              accessibilityLabel="Toggle notifications"
            />
          )}
        />
      </List.Section>

      {darkMode ? (
        <Text variant="bodySmall" style={styles.note}>
          Dark theme is a saved preference; the full dark palette is applied in a later task.
        </Text>
      ) : null}

      <View style={styles.logout}>
        <AppButton label="Log out" variant="secondary" onPress={signOut} />
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingBottom: spacing.xl },
  note: { color: palette.textSecondary, paddingHorizontal: spacing.md, paddingTop: spacing.xs },
  logout: { padding: spacing.md },
});
