import { z } from 'zod';

/**
 * Client-side auth validation (HLD §9, §23). Mirrors the backend contract so
 * users get fast feedback, but the backend remains authoritative — client
 * validation is not a security boundary.
 */

export const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'Email is required')
    .max(254, 'Email must be at most 254 characters')
    .email('Enter a valid email address'),
  password: z
    .string()
    .min(1, 'Password is required')
    .min(8, 'Password must be at least 8 characters')
    .max(128, 'Password must be at most 128 characters'),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

/**
 * Returns true if the string is a time zone recognized by the runtime. Uses
 * `Intl.supportedValuesOf('timeZone')` when available; otherwise falls back to
 * attempting to construct a formatter with the zone.
 */
function isValidIanaTimeZone(tz: string): boolean {
  try {
    const intlAny = Intl as unknown as { supportedValuesOf?: (k: string) => string[] };
    if (typeof intlAny.supportedValuesOf === 'function') {
      return intlAny.supportedValuesOf('timeZone').includes(tz);
    }
    // Fallback: throws for an invalid zone.
    new Intl.DateTimeFormat('en-US', { timeZone: tz });
    return true;
  } catch {
    return false;
  }
}

export const registerSchema = z
  .object({
    displayName: z
      .string()
      .min(1, 'Name is required')
      .max(100, 'Name must be at most 100 characters'),
    email: z
      .string()
      .min(1, 'Email is required')
      .max(254, 'Email must be at most 254 characters')
      .email('Enter a valid email address'),
    password: z
      .string()
      .min(1, 'Password is required')
      .min(8, 'Password must be at least 8 characters')
      .max(128, 'Password must be at most 128 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
    // Optional: empty string means "use the backend default" (INR / Asia/Kolkata).
    currency: z
      .string()
      .trim()
      .regex(/^[A-Z]{3}$/, 'Use a 3-letter uppercase ISO code (e.g. INR)')
      .optional()
      .or(z.literal('')),
    timezone: z
      .string()
      .trim()
      .refine((tz) => tz === '' || isValidIanaTimeZone(tz), {
        message: 'Enter a valid IANA time zone (e.g. Asia/Kolkata)',
      })
      .optional(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

export type RegisterFormValues = z.infer<typeof registerSchema>;
