import { http } from './client';

/**
 * Expense API (services/api layer). Mirrors the backend ExpenseResponse and
 * Create/UpdateExpenseRequest DTOs. The shared client unwraps the envelope and
 * attaches the Bearer token, so these return the data payload directly.
 */

export type ExpenseDto = {
  id: string;
  amount: number;
  /** ISO date (YYYY-MM-DD). */
  expenseDate: string;
  description: string | null;
  notes: string | null;
  categoryId: string | null;
  paymentMethodId: string | null;
  createdAt: string;
};

/** Body for create and update (the two backend DTOs have identical fields). */
export type ExpenseInput = {
  amount: number;
  expenseDate: string;
  description?: string;
  notes?: string;
  categoryId?: string;
  paymentMethodId?: string;
};

/** GET /expenses */
export async function listExpenses(): Promise<ExpenseDto[]> {
  const res = await http.get<ExpenseDto[]>('/expenses');
  return res.data;
}

/** GET /expenses/{id} */
export async function getExpense(id: string): Promise<ExpenseDto> {
  const res = await http.get<ExpenseDto>(`/expenses/${id}`);
  return res.data;
}

/** POST /expenses */
export async function createExpense(body: ExpenseInput): Promise<ExpenseDto> {
  const res = await http.post<ExpenseDto>('/expenses', body);
  return res.data;
}

/** PUT /expenses/{id} */
export async function updateExpense(id: string, body: ExpenseInput): Promise<ExpenseDto> {
  const res = await http.put<ExpenseDto>(`/expenses/${id}`, body);
  return res.data;
}
