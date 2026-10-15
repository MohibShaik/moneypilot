import { create } from 'zustand';

/**
 * Global toast/snackbar state. A single snackbar is shown app-wide; calling
 * show() replaces any current message. Kept out of component state so any
 * screen or hook can trigger a toast without prop-drilling.
 */
export type ToastTone = 'success' | 'error' | 'info';

type ToastState = {
  visible: boolean;
  message: string;
  tone: ToastTone;
  show: (message: string, tone?: ToastTone) => void;
  hide: () => void;
};

export const useToastStore = create<ToastState>((set) => ({
  visible: false,
  message: '',
  tone: 'info',
  show: (message, tone = 'info') => set({ visible: true, message, tone }),
  hide: () => set({ visible: false }),
}));
