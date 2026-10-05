/**
 * Payment DTO types (HLD §14, §20). Shaped to resemble the future
 * `/api/v1/payments` responses and aligned with the backend V2 schema (explicit
 * payment_type with loan_emi_id / card_statement_id). Payments are NOT expenses
 * (HLD §14). All amounts (including authoritative "remaining") are
 * backend-computed; the client never calculates them (HLD §4.1).
 */

export type PaymentType = 'LOAN_EMI' | 'CREDIT_CARD' | 'OTHER';

export type Payment = {
  id: string;
  paymentType: PaymentType;
  /** What was paid, e.g. "Home Loan EMI #3" or "HDFC Regalia statement". */
  label: string;
  amount: number;
  /** ISO date (YYYY-MM-DD). */
  paidAt: string;
  paymentMethodLabel: string | null;
  currency: string;
};

/**
 * A payable target the user can pay toward (a loan EMI or a card statement).
 * The `remaining` value is authoritative and comes from the backend; the form
 * displays it and allows partial payments up to that amount.
 */
export type PayableTarget = {
  id: string;
  kind: Extract<PaymentType, 'LOAN_EMI' | 'CREDIT_CARD'>;
  /** Loan/card name for context. */
  title: string;
  /** EMI amount or statement amount (backend-computed). */
  totalAmount: number;
  /** Already paid (backend-computed; partial payments supported). */
  alreadyPaid: number;
  /** Authoritative remaining amount (backend-computed). */
  remaining: number;
  currency: string;
};

export type PaymentGroup = {
  /** ISO date (YYYY-MM-DD) the group covers. */
  date: string;
  payments: Payment[];
};
