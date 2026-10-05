import { z } from 'zod';

/**
 * Credit card form validation (HLD §23). Mirrors the backend V2 constraints
 * (credit_limit > 0, statement_day / payment_due_day 1–31, status enum,
 * last_four 4 digits). The client never stores full PAN/CVV/PIN (HLD §24).
 */

const CARD_STATUSES = ['ACTIVE', 'BLOCKED', 'CLOSED'] as const;

const positiveAmount = z
  .string()
  .min(1, 'Required')
  .refine((v) => Number.isFinite(Number(v)) && Number(v) > 0, 'Enter an amount greater than 0');

/** 1–31 day-of-month string. */
const dayOfMonth = z
  .string()
  .min(1, 'Required')
  .refine((v) => /^\d{1,2}$/.test(v) && Number(v) >= 1 && Number(v) <= 31, 'Enter a day from 1 to 31');

export const cardFormSchema = z.object({
  cardName: z.string().min(1, 'Card name is required').max(120, 'Name is too long'),
  bank: z.string().min(1, 'Bank is required').max(100, 'Bank name is too long'),
  lastFour: z
    .string()
    .regex(/^\d{4}$/, 'Enter the last 4 digits'),
  creditLimit: positiveAmount,
  statementDay: dayOfMonth,
  paymentDueDay: dayOfMonth,
  status: z.enum(CARD_STATUSES, { message: 'Select a status' }),
});

export type CardFormValues = z.infer<typeof cardFormSchema>;

export const CARD_STATUS_OPTIONS = CARD_STATUSES.map((v) => ({
  value: v,
  label: v.charAt(0) + v.slice(1).toLowerCase(),
}));
