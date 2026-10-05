/**
 * Shared non-spacing dimension tokens (HLD §16): border radii, border widths,
 * and minimum touch-target size (accessibility, HLD §25).
 */
export const radius = {
  sm: 8,
  md: 12,
  lg: 16,
  /** Card corners — the "premium" 14–18px range from the visual direction. */
  card: 16,
  pill: 999,
} as const;

export const borderWidth = {
  hairline: 1,
  thick: 2,
} as const;

export const dimensions = {
  /** Minimum touch target (accessibility, HLD §25). */
  minTouchTarget: 44,
  /** Default control height. */
  controlHeight: 48,
  iconSize: 24,
} as const;

export type RadiusKey = keyof typeof radius;
