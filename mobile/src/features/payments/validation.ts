import { z } from 'zod';

/**
 * Payment form validation (HLD §14, §23). The amount must be > 0 and no greater
 * than the authoritative remaining amount supplied by the backend (partial
 * payments are supported). The backend remains the source of truth for the
 * remaining amount and the final settlement.
 */
export function makePaymentSchema(remaining: number) {
  return z.object({
    amount: z
      .string()
      .min(1, 'Amount is required')
      .refine((v) => Number.isFinite(Number(v)) && Number(v) > 0, 'Enter an amount greater than 0')
      .refine(
        (v) => Number(v) <= remaining,
        `Amount cannot exceed the remaining ${remaining.toFixed(2)}`,
      ),
    paymentMethodId: z.string().min(1, 'Select a payment method'),
    paymentDate: z
      .string()
      .min(1, 'Date is required')
      .regex(/^\d{4}-\d{2}-\d{2}$/, 'Use YYYY-MM-DD'),
  });
}

export type PaymentFormValues = {
  amount: string;
  paymentMethodId: string;
  paymentDate: string;
};
