import type { PayableTarget, Payment } from '../types';

/**
 * Mock payment data for UI-first development (HLD §20). Isolated so it can be
 * replaced by `/api/v1/payments` responses without touching the screens.
 * Amounts (including `remaining`) resemble the eventual API DTO and are never
 * computed on the client (HLD §4.1).
 */

export const mockPayments: Payment[] = [
  {
    id: 'pay-1',
    paymentType: 'CREDIT_CARD',
    label: 'HDFC Regalia statement',
    amount: 20000,
    paidAt: '2026-10-02',
    paymentMethodLabel: 'UPI',
    currency: 'INR',
  },
  {
    id: 'pay-2',
    paymentType: 'LOAN_EMI',
    label: 'Home Loan EMI #3',
    amount: 10000,
    paidAt: '2026-10-01',
    paymentMethodLabel: 'Bank transfer',
    currency: 'INR',
  },
  {
    id: 'pay-3',
    paymentType: 'OTHER',
    label: 'Miscellaneous',
    amount: 500,
    paidAt: '2026-09-28',
    paymentMethodLabel: 'Cash',
    currency: 'INR',
  },
];

export function findMockPayment(id: string): Payment | undefined {
  return mockPayments.find((p) => p.id === id);
}

/** Payable targets keyed by id (authoritative remaining comes from backend). */
const payableTargets: Record<string, PayableTarget> = {
  'emi-3': {
    id: 'emi-3',
    kind: 'LOAN_EMI',
    title: 'Home Loan · EMI #3',
    totalAmount: 28500,
    alreadyPaid: 10000,
    remaining: 18500,
    currency: 'INR',
  },
  'stmt-1': {
    id: 'stmt-1',
    kind: 'CREDIT_CARD',
    title: 'HDFC Regalia · Statement',
    totalAmount: 50000,
    alreadyPaid: 20000,
    remaining: 30000,
    currency: 'INR',
  },
};

export function findMockPayableTarget(id: string): PayableTarget | undefined {
  return payableTargets[id];
}
