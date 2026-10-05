/**
 * Color palette for the MoneyPilot design system (HLD §16).
 *
 * Screens must not hardcode colors; they consume these tokens (directly or via
 * the Paper theme in `theme.ts`). Status colors are provided for convenience,
 * but status must never be conveyed by color alone (HLD §11, §25) — always pair
 * with text/icon.
 */
export const palette = {
  // Brand — reference palette: near-black charcoal primary with a violet accent.
  primary: '#1C202A', // charcoal (hero cards, primary actions)
  primaryDark: '#12151C',
  primaryLight: '#ECECF9', // tint of the violet accent for containers
  accent: '#625FD4', // violet
  secondary: '#625FD4',
  heading: '#25282A', // headings

  // Neutrals
  black: '#000000',
  white: '#FFFFFF',
  background: '#F7F8FA', // very light neutral gray
  surface: '#FFFFFF',
  outline: '#EBECED', // subtle card borders

  // Text (meet contrast guidance in §25)
  textPrimary: '#25282A', // heading-strength body text
  textSecondary: '#ADADAD', // text / secondary
  textDisabled: '#D0D0D0', // muted
  textOnPrimary: '#FFFFFF',

  // Feedback
  error: '#E51414', // red — errors and negative amounts
  success: '#23BF5D', // green — positive financial states
  warning: '#F59E0B', // amber — due / upcoming (distinct from error red)
  info: '#625FD4', // violet
} as const;

/**
 * Semantic status colors for financial states (loans/EMIs/cards/payments).
 * Keep in sync with backend enums; always accompany with a label.
 */
export const statusColors = {
  PAID: palette.success, // green
  DUE: palette.warning, // amber
  UPCOMING: palette.warning, // amber (upcoming/due share the amber "attention" tone)
  OVERDUE: palette.error, // red
  WAIVED: palette.textSecondary,

  ACTIVE: palette.success,
  CLOSED: palette.textSecondary,
  FORECLOSED: palette.warning,

  GENERATED: palette.info,
  PARTIALLY_PAID: palette.warning,
} as const;

export type StatusKey = keyof typeof statusColors;
