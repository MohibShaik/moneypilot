import { z } from 'zod';

/**
 * Edit-profile validation (HLD §15, §23). Mirrors backend intent: display name
 * required (1–100); currency optional (3 uppercase letters); timezone optional
 * (valid IANA zone). Email is not editable here. Backend remains authoritative.
 */

function isValidIanaTimeZone(tz: string): boolean {
  try {
    const intlAny = Intl as unknown as { supportedValuesOf?: (k: string) => string[] };
    if (typeof intlAny.supportedValuesOf === 'function') {
      return intlAny.supportedValuesOf('timeZone').includes(tz);
    }
    new Intl.DateTimeFormat('en-US', { timeZone: tz });
    return true;
  } catch {
    return false;
  }
}

export const editProfileSchema = z.object({
  displayName: z
    .string()
    .min(1, 'Name is required')
    .max(100, 'Name must be at most 100 characters'),
  currency: z
    .string()
    .trim()
    .regex(/^[A-Z]{3}$/, 'Use a 3-letter uppercase ISO code (e.g. INR)'),
  timezone: z
    .string()
    .trim()
    .refine(isValidIanaTimeZone, { message: 'Enter a valid IANA time zone (e.g. Asia/Kolkata)' }),
});

export type EditProfileFormValues = z.infer<typeof editProfileSchema>;
