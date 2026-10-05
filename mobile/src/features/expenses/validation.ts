import { z } from 'zod';

/**
 * Expense form validation (HLD §13, §23). Mirrors backend intent: amount > 0,
 * valid category and payment method, valid date; description and notes
 * optional. The backend remains authoritative.
 */

const positiveAmount = z
  .string()
  .min(1, 'Amount is required')
  .refine((v) => Number.isFinite(Number(v)) && Number(v) > 0, 'Enter an amount greater than 0');

export const expenseFormSchema = z.object({
  amount: positiveAmount,
  categoryId: z.string().min(1, 'Select a category'),
  paymentMethodId: z.string().min(1, 'Select a payment method'),
  expenseDate: z
    .string()
    .min(1, 'Date is required')
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Use YYYY-MM-DD'),
  description: z.string().max(255, 'Description is too long').optional().or(z.literal('')),
  notes: z.string().max(1000, 'Notes are too long').optional().or(z.literal('')),
});

export type ExpenseFormValues = z.infer<typeof expenseFormSchema>;
