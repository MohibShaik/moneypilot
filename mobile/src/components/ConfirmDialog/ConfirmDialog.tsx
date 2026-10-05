import { Button, Dialog, Portal, Text } from 'react-native-paper';

import { palette } from '@/theme/theme';

export type ConfirmDialogProps = {
  visible: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  /** Marks the confirm action as destructive (e.g. logout, delete). */
  destructive?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

/**
 * Reusable confirmation dialog (HLD §17), e.g. for logout or destructive
 * actions. Rendered in a Portal so it overlays the current screen.
 */
export function ConfirmDialog({
  visible,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  destructive = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Portal>
      <Dialog visible={visible} onDismiss={onCancel}>
        <Dialog.Title>{title}</Dialog.Title>
        <Dialog.Content>
          <Text variant="bodyMedium">{message}</Text>
        </Dialog.Content>
        <Dialog.Actions>
          <Button onPress={onCancel}>{cancelLabel}</Button>
          <Button onPress={onConfirm} textColor={destructive ? palette.error : undefined}>
            {confirmLabel}
          </Button>
        </Dialog.Actions>
      </Dialog>
    </Portal>
  );
}
