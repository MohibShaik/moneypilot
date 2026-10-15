import { http } from './client';

export type PaymentMethodTypeDto =
  | 'CASH'
  | 'UPI'
  | 'CREDIT_CARD'
  | 'DEBIT_CARD'
  | 'BANK_TRANSFER'
  | 'OTHER';

/** Mirrors the backend PaymentMethodResponse DTO. */
export type PaymentMethodDto = {
  id: string;
  type: PaymentMethodTypeDto;
  label: string;
  creditCardId: string | null;
  createdAt: string;
};

/**
 * Body for POST /payment-methods. creditCardId is only valid when
 * type === CREDIT_CARD; this UI does not create CREDIT_CARD methods yet, so it
 * is left out.
 */
export type CreatePaymentMethodInput = {
  type: PaymentMethodTypeDto;
  label: string;
};

/** GET /payment-methods */
export async function listPaymentMethods(): Promise<PaymentMethodDto[]> {
  const res = await http.get<PaymentMethodDto[]>('/payment-methods');
  return res.data;
}

/** POST /payment-methods */
export async function createPaymentMethod(
  body: CreatePaymentMethodInput,
): Promise<PaymentMethodDto> {
  const res = await http.post<PaymentMethodDto>('/payment-methods', body);
  return res.data;
}
