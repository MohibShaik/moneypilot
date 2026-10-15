import { z } from 'zod';

/**
 * Loan form validation (HLD §23). Mirrors the backend V2 constraints so users
 * get fast feedback; the backend remains authoritative. Numeric fields arrive
 * as strings from the inputs and are validated as positive/non-negative
 * decimals (string form is kept for controlled inputs; parsed on submit).
 */

const LOAN_TYPES = ['HOME', 'CAR', 'PERSONAL', 'EDUCATION', 'GOLD', 'OTHER'] as const;
const LOAN_STATUSES = ['ACTIVE', 'CLOSED', 'FORECLOSED'] as const;

/** A required positive-decimal string (> 0). */
const positiveAmount = z
  .string()
  .min(1, 'Required')
  .refine((v) => Number.isFinite(Number(v)) && Number(v) > 0, 'Enter an amount greater than 0');

/** A required non-negative decimal string (>= 0). */
const nonNegativeAmount = z
  .string()
  .min(1, 'Required')
  .refine((v) => Number.isFinite(Number(v)) && Number(v) >= 0, 'Enter 0 or more');

/** A required positive integer string. */
const positiveInt = z
  .string()
  .min(1, 'Required')
  .refine((v) => /^\d+$/.test(v) && Number(v) > 0, 'Enter a whole number greater than 0');

export const loanFormSchema = z.object({
  name: z.string().min(1, 'Name is required').max(120, 'Name is too long'),
  lender: z.string().max(150, 'Lender name is too long').optional().or(z.literal('')),
  loanType: z.enum(LOAN_TYPES, { message: 'Select a loan type' }),
  principalAmount: positiveAmount,
  // outstandingPrincipal is derived server-side on create; optional in the form.
  outstandingPrincipal: nonNegativeAmount.optional().or(z.literal('')),
  // emiAmount is optional: the backend computes it when omitted.
  emiAmount: positiveAmount.optional().or(z.literal('')),
  interestRate: nonNegativeAmount,
  startDate: z
    .string()
    .min(1, 'Start date is required')
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Use YYYY-MM-DD'),
  endDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Use YYYY-MM-DD')
    .optional()
    .or(z.literal('')),
  tenureMonths: positiveInt,
  status: z.enum(LOAN_STATUSES, { message: 'Select a status' }),
});

export type LoanFormValues = z.infer<typeof loanFormSchema>;

export const LOAN_TYPE_OPTIONS = LOAN_TYPES.map((v) => ({
  value: v,
  label: v.charAt(0) + v.slice(1).toLowerCase(),
}));

export const LOAN_STATUS_OPTIONS = LOAN_STATUSES.map((v) => ({
  value: v,
  label: v.charAt(0) + v.slice(1).toLowerCase(),
}));
