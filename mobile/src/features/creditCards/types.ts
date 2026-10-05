/**
 * Credit card DTO types (HLD §12, §20). Shaped to resemble the future
 * `/api/v1/credit-cards` responses and aligned with the backend V2 schema
 * (card status, statement amounts/status). Only `lastFour` is ever present —
 * never full card number, CVV, or PIN (HLD §12, §24). All monetary values are
 * backend-computed (HLD §4.1).
 */

export type CardStatus = 'ACTIVE' | 'BLOCKED' | 'CLOSED';

export type StatementStatus = 'GENERATED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE';

/** Row shape for the card list (HLD §12 "Card List"). */
export type CardSummary = {
  id: string;
  cardName: string;
  bank: string;
  /** Last four digits only — never the full PAN. */
  lastFour: string;
  creditLimit: number;
  /** Backend-computed available/used amounts, when supplied. */
  availableAmount: number | null;
  usedAmount: number | null;
  /** ISO date (YYYY-MM-DD) of the current statement's due date, or null. */
  dueDate: string | null;
  status: CardStatus;
  currency: string;
};

/** A billing statement (HLD §12 "Statement History"). */
export type CardStatement = {
  id: string;
  /** ISO dates (YYYY-MM-DD). */
  statementDate: string;
  dueDate: string;
  statementAmount: number;
  minimumDue: number;
  /** Backend-computed amount paid (partial payments allowed). */
  paidAmount: number;
  status: StatementStatus;
};

/** Full card detail (HLD §12 "Card Details"). */
export type CardDetail = {
  id: string;
  cardName: string;
  bank: string;
  lastFour: string;
  creditLimit: number;
  availableAmount: number | null;
  usedAmount: number | null;
  status: CardStatus;
  currency: string;
  /** Most recent statement, if any. */
  currentStatement: CardStatement | null;
  /** Prior statements, newest first. */
  statementHistory: CardStatement[];
};
