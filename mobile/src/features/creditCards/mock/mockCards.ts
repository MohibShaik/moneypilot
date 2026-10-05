import type { CardDetail, CardStatement, CardSummary } from '../types';

/**
 * Mock credit card data for UI-first development (HLD §20). Isolated so it can
 * be replaced by `/api/v1/credit-cards` responses without touching the screens.
 * Only last-four is stored; values resemble the eventual API DTO and are never
 * computed on the client (HLD §4.1, §24).
 */

export const mockCards: CardSummary[] = [
  {
    id: 'card-1',
    cardName: 'HDFC Regalia',
    bank: 'HDFC Bank',
    lastFour: '4521',
    creditLimit: 200000,
    availableAmount: 135000,
    usedAmount: 65000,
    dueDate: '2026-10-15',
    status: 'ACTIVE',
    currency: 'INR',
  },
  {
    id: 'card-2',
    cardName: 'ICICI Amazon Pay',
    bank: 'ICICI Bank',
    lastFour: '8830',
    creditLimit: 150000,
    availableAmount: 150000,
    usedAmount: 0,
    dueDate: null,
    status: 'ACTIVE',
    currency: 'INR',
  },
];

const currentStatement: CardStatement = {
  id: 'stmt-1',
  statementDate: '2026-09-20',
  dueDate: '2026-10-15',
  statementAmount: 50000,
  minimumDue: 2500,
  paidAmount: 20000,
  status: 'PARTIALLY_PAID',
};

const history: CardStatement[] = [
  {
    id: 'stmt-0',
    statementDate: '2026-08-20',
    dueDate: '2026-09-15',
    statementAmount: 42000,
    minimumDue: 2100,
    paidAmount: 42000,
    status: 'PAID',
  },
];

const cardDetails: Record<string, CardDetail> = {
  'card-1': {
    id: 'card-1',
    cardName: 'HDFC Regalia',
    bank: 'HDFC Bank',
    lastFour: '4521',
    creditLimit: 200000,
    availableAmount: 135000,
    usedAmount: 65000,
    status: 'ACTIVE',
    currency: 'INR',
    currentStatement,
    statementHistory: history,
  },
  'card-2': {
    id: 'card-2',
    cardName: 'ICICI Amazon Pay',
    bank: 'ICICI Bank',
    lastFour: '8830',
    creditLimit: 150000,
    availableAmount: 150000,
    usedAmount: 0,
    status: 'ACTIVE',
    currency: 'INR',
    currentStatement: null,
    statementHistory: [],
  },
};

export function findMockCardDetail(id: string): CardDetail | undefined {
  return cardDetails[id];
}
