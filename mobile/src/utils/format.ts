/**
 * Pure presentation formatters. No financial business logic lives here — the
 * backend owns authoritative calculations (HLD §4.1). These helpers only format
 * already-computed values for display.
 */

/**
 * Formats a numeric amount as a currency string using Intl.
 *
 * @param value   the amount (already computed by the backend / mock)
 * @param currency ISO 4217 code, defaults to INR
 * @param locale  BCP-47 locale, defaults to en-IN
 */
export function formatCurrency(
  value: number,
  currency = 'INR',
  locale = 'en-IN',
): string {
  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      maximumFractionDigits: 2,
    }).format(value);
  } catch {
    // Fallback if the runtime lacks full Intl currency data.
    return `${currency} ${value.toFixed(2)}`;
  }
}

/** Masks a card to its last four digits for display (never store full PAN). */
export function maskCardLastFour(lastFour: string): string {
  return `•••• ${lastFour}`;
}

/**
 * Formats an ISO date (YYYY-MM-DD) as a short, human-readable date, e.g.
 * "Oct 10". Presentation only. Returns the input unchanged if it can't parse.
 */
export function formatShortDate(isoDate: string, locale = 'en-IN'): string {
  const parsed = new Date(`${isoDate}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) {
    return isoDate;
  }
  try {
    return new Intl.DateTimeFormat(locale, { month: 'short', day: 'numeric' }).format(parsed);
  } catch {
    return isoDate;
  }
}

/**
 * Returns a time-of-day greeting ("Good morning", etc.) for the given hour
 * (defaults to now). Presentation only.
 */
export function greetingForHour(hour = new Date().getHours()): string {
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}
