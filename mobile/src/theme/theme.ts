import { MD3LightTheme, type MD3Theme } from 'react-native-paper';

import { palette } from './colors';

/**
 * The MoneyPilot React Native Paper theme (HLD §16).
 *
 * Built on the Material Design 3 light theme with the brand palette applied.
 * Screens and components should consume theme values (via `useTheme()` or the
 * exported tokens) rather than hardcoding design values.
 */
export const appTheme: MD3Theme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    primary: palette.primary, // charcoal
    onPrimary: palette.textOnPrimary,
    primaryContainer: palette.primaryLight,
    onPrimaryContainer: palette.accent,
    secondary: palette.secondary, // violet accent
    onSecondary: palette.textOnPrimary,
    tertiary: palette.success, // green for positive states
    background: palette.background,
    onBackground: palette.textPrimary,
    surface: palette.surface,
    surfaceVariant: palette.primaryLight,
    onSurface: palette.textPrimary,
    onSurfaceVariant: palette.textSecondary,
    outline: palette.outline,
    outlineVariant: palette.outline,
    error: palette.error,
  },
};

export { palette, statusColors } from './colors';
export { spacing } from './spacing';
export { typography } from './typography';
export { radius, borderWidth, dimensions } from './dimensions';
export type { StatusKey } from './colors';
export type { TypographyVariant } from './typography';

export type AppTheme = typeof appTheme;
