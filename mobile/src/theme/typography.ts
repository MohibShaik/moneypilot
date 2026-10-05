import type { TextStyle } from 'react-native';

/**
 * Semantic typography levels (HLD §16): display, headline, title, body, label,
 * caption. Screens reference these instead of hardcoding font sizes/weights.
 *
 * Typeface: the visual direction calls for an Inter / SF Pro-style face. React
 * Native defaults to the platform system font (SF Pro on iOS, Roboto on
 * Android), which satisfies the "system typography" intent without bundling a
 * font. If we later want Inter everywhere (incl. web/Android), load it via
 * expo-font and set it on the Paper theme's `fonts` — screens consuming these
 * tokens won't need changes.
 */
export const typography = {
  display: { fontSize: 34, lineHeight: 40, fontWeight: '700' },
  headline: { fontSize: 28, lineHeight: 34, fontWeight: '700' },
  title: { fontSize: 20, lineHeight: 26, fontWeight: '600' },
  body: { fontSize: 16, lineHeight: 22, fontWeight: '400' },
  label: { fontSize: 14, lineHeight: 18, fontWeight: '600' },
  caption: { fontSize: 12, lineHeight: 16, fontWeight: '400' },
} as const satisfies Record<string, TextStyle>;

export type TypographyVariant = keyof typeof typography;
