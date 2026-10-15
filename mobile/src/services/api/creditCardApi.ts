import { http } from './client';

/**
 * Credit card + statement API (services/api layer). Mirrors the backend
 * CreditCardResponse and CardStatementResponse DTOs. Statements are nested
 * under a card: /credit-cards/{cardId}/statements.
 */

export type CardStatus = 'ACTIVE' | 'BLOCKED' | 'CLOSED';
export type StatementStatus = 'GENERATED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE';

export type CreditCardDto = {
  id: string;
  cardName: string;
  bank: string;
  lastFour: string;
  creditLimit: number;
  statementDay: number;
  paymentDueDay: number;
  status: CardStatus;
  createdAt: string;
};

export type CardStatementDto = {
  id: string;
  creditCardId: string;
  statementDate: string;
  dueDate: string;
  statementAmount: number;
  minimumDue: number;
  paidAmount: number;
  status: StatementStatus;
  createdAt: string;
};

/** Create body. Status defaults to ACTIVE server-side. */
export type CreateCreditCardInput = {
  cardName: string;
  bank: string;
  lastFour: string;
  creditLimit: number;
  statementDay: number;
  paymentDueDay: number;
};

/** Update body. lastFour is immutable and not accepted. */
export type UpdateCreditCardInput = {
  cardName: string;
  bank: string;
  creditLimit: number;
  statementDay: number;
  paymentDueDay: number;
  status: CardStatus;
};

export async function listCreditCards(): Promise<CreditCardDto[]> {
  const res = await http.get<CreditCardDto[]>('/credit-cards');
  return res.data;
}

export async function getCreditCard(id: string): Promise<CreditCardDto> {
  const res = await http.get<CreditCardDto>(`/credit-cards/${id}`);
  return res.data;
}

export async function createCreditCard(body: CreateCreditCardInput): Promise<CreditCardDto> {
  const res = await http.post<CreditCardDto>('/credit-cards', body);
  return res.data;
}

export async function updateCreditCard(id: string, body: UpdateCreditCardInput): Promise<CreditCardDto> {
  const res = await http.put<CreditCardDto>(`/credit-cards/${id}`, body);
  return res.data;
}

/** GET /credit-cards/{cardId}/statements */
export async function listCardStatements(cardId: string): Promise<CardStatementDto[]> {
  const res = await http.get<CardStatementDto[]>(`/credit-cards/${cardId}/statements`);
  return res.data;
}
