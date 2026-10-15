import { useCallback } from 'react';
import { StyleSheet } from 'react-native';
import { Snackbar } from 'react-native-paper';

import { useToastStore, type ToastTone } from '@/store/toastStore';
import { palette } from '@/theme/theme';

const TONE_COLOR: Record<ToastTone, string> = {
  success: palette.success,
  error: palette.error,
  info: palette.primary,
};

/**
 * App-wide snackbar host (HLD 22). Rendered once near the app root; it listens
 * to the toast store and shows the current message. Any screen or hook can
 * trigger it via useToast(). Auto-dismisses after a few seconds.
 */
export function ToastHost() {
  const { visible, message, tone, hide } = useToastStore();
  return (
    <Snackbar
      visible={visible}
      onDismiss={hide}
      duration={3000}
      style={[styles.bar, { backgroundColor: TONE_COLOR[tone] }]}
      action={{ label: 'Dismiss', onPress: hide }}
    >
      {message}
    </Snackbar>
  );
}

/** Imperative toast API usable from any component or callback. */
export function useToast() {
  const show = useToastStore((s) => s.show);
  const showSuccess = useCallback((msg: string) => show(msg, 'success'), [show]);
  const showError = useCallback((msg: string) => show(msg, 'error'), [show]);
  const showInfo = useCallback((msg: string) => show(msg, 'info'), [show]);
  return { showSuccess, showError, showInfo };
}

const styles = StyleSheet.create({
  bar: { marginBottom: 8 },
});
