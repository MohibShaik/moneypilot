import { http } from './client';

/**
 * Payment API (services/api layer). Mirrors the backend PaymentResponse and
 * CreatePaymentRequest DTOs (/api/v1/payments). The shared {@link http} client
 * unwraps the envelope and attaches the Bearer token, so these return the
 * `data` payload directly.
 *
 * Payments are immutable: there is no update endpoint. Correct a payment with
 * delete + re-create (DELETE /payments/{id} then POST /payments).
 */

export type PaymentType = 'LOAN_EMI' | 'CREDIT_CARD' | 'OTHER';

/** Mirrors the backend PaymentResponse record (flat; raw target ids). */
export type PaymentDto = {
  id: string;
  type: PaymentType;
  amount: number;
  /** ISO date-time (OffsetDateTime). */
  paidAt: string;
  loanEmiId: string | null;
  cardStatementId: string | null;
  paymentMethodId: string | null;
  createdAt: string;
};

/**
 * Body for POST /payments. The target id required depends on `type` (enforced
 * server-side): LOAN_EMI needs loanEmiId; CREDIT_CARD needs cardStatementId;
 * OTHER needs neither. paymentMethodId is optional for any type. paidAt
 * defaults to now when omitted.
 */
export type CreatePaymentInput = {
  type: PaymentType;
  amount: number;
  /** ISO date-time; defaults to now server-side when omitted. */
  paidAt?: string;
  loanEmiId?: string;
  cardStatementId?: string;
  paymentMethodId?: string;
};

/** Optional filters for GET /payments (from/to apply to paidAt). */
export type ListPaymentsParams = {
  type?: PaymentType;
  /** ISO date-time. */
  from?: string;
  /** ISO date-time. */
  to?: string;
};

/** GET /payments — optional type/from/to filters on paidAt. */
export async function listPayments(params?: ListPaymentsParams): Promise<PaymentDto[]> {
  const res = await http.get<PaymentDto[]>('/payments', { params });
  return res.data;
}

/** GET /payments/{id} */
export async function getPayment(id: string): Promise<PaymentDto> {
  const res = await http.get<PaymentDto>(`/payments/${id}`);
  return res.data;
}

/** POST /payments — records a payment and settles the targeted EMI/statement. */
export async function createPayment(body: CreatePaymentInput): Promise<PaymentDto> {
  const res = await http.post<PaymentDto>('/payments', body);
  return res.data;
}

/** DELETE /payments/{id} — removes the payment and re-settles its target. */
export async function deletePayment(id: string): Promise<void> {
  await http.delete(`/payments/${id}`);
}
