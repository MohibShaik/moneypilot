/**
 * Semantic spacing scale (HLD §16). Use these tokens for padding, margin, and
 * gaps instead of hardcoded numbers.
 */
export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
} as const;

export type SpacingKey = keyof typeof spacing;
