/**
 * Expense DTO types (HLD §13, §20). Shaped to resemble the future
 * `/api/v1/expenses`, `/api/v1/categories`, and payment-method responses, and
 * aligned with the backend V2 schema. Amounts are backend-stored values; the
 * client never computes authoritative financial state (HLD §4.1).
 */

export type CategoryType = 'EXPENSE' | 'INCOME';

export type Category = {
  id: string;
  name: string;
  categoryType: CategoryType;
  /** null = system/default category; set = user-owned (backend V2 semantics). */
  userId: string | null;
};

export type PaymentMethodType =
  | 'CASH'
  | 'UPI'
  | 'CREDIT_CARD'
  | 'DEBIT_CARD'
  | 'BANK_TRANSFER'
  | 'OTHER';

export type PaymentMethod = {
  id: string;
  label: string;
  type: PaymentMethodType;
};

export type Expense = {
  id: string;
  amount: number;
  /** ISO date (YYYY-MM-DD). */
  expenseDate: string;
  categoryId: string | null;
  categoryName: string | null;
  paymentMethodId: string | null;
  paymentMethodLabel: string | null;
  description: string | null;
  notes: string | null;
  currency: string;
};

/** A date bucket for the grouped expense list (HLD §13). */
export type ExpenseGroup = {
  /** ISO date (YYYY-MM-DD) the group covers. */
  date: string;
  expenses: Expense[];
};
