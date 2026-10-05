import type { Category, Expense, PaymentMethod } from '../types';

/**
 * Mock expense data for UI-first development (HLD §20). Isolated so it can be
 * replaced by `/api/v1/expenses`, `/api/v1/categories`, and payment-method
 * responses without touching the screens. Values resemble the eventual API DTOs
 * and are never computed on the client (HLD §4.1).
 */

export const mockCategories: Category[] = [
  { id: 'cat-food', name: 'Food', categoryType: 'EXPENSE', userId: null },
  { id: 'cat-shopping', name: 'Shopping', categoryType: 'EXPENSE', userId: null },
  { id: 'cat-fuel', name: 'Fuel', categoryType: 'EXPENSE', userId: null },
  { id: 'cat-bills', name: 'Bills', categoryType: 'EXPENSE', userId: null },
  { id: 'cat-travel', name: 'Travel', categoryType: 'EXPENSE', userId: 'user-1' },
];

export const mockPaymentMethods: PaymentMethod[] = [
  { id: 'pm-upi', label: 'UPI', type: 'UPI' },
  { id: 'pm-cash', label: 'Cash', type: 'CASH' },
  { id: 'pm-hdfc', label: 'HDFC Regalia', type: 'CREDIT_CARD' },
  { id: 'pm-debit', label: 'Debit card', type: 'DEBIT_CARD' },
];

export const mockExpenses: Expense[] = [
  {
    id: 'exp-1',
    amount: 450,
    expenseDate: '2026-10-03',
    categoryId: 'cat-food',
    categoryName: 'Food',
    paymentMethodId: 'pm-upi',
    paymentMethodLabel: 'UPI',
    description: 'Lunch',
    notes: null,
    currency: 'INR',
  },
  {
    id: 'exp-2',
    amount: 2500,
    expenseDate: '2026-10-03',
    categoryId: 'cat-shopping',
    categoryName: 'Shopping',
    paymentMethodId: 'pm-hdfc',
    paymentMethodLabel: 'HDFC Regalia',
    description: 'Clothing',
    notes: null,
    currency: 'INR',
  },
  {
    id: 'exp-3',
    amount: 2000,
    expenseDate: '2026-10-02',
    categoryId: 'cat-fuel',
    categoryName: 'Fuel',
    paymentMethodId: 'pm-debit',
    paymentMethodLabel: 'Debit card',
    description: 'Petrol',
    notes: null,
    currency: 'INR',
  },
  {
    id: 'exp-4',
    amount: 1200,
    expenseDate: '2026-10-01',
    categoryId: 'cat-bills',
    categoryName: 'Bills',
    paymentMethodId: 'pm-upi',
    paymentMethodLabel: 'UPI',
    description: 'Electricity',
    notes: 'October cycle',
    currency: 'INR',
  },
];

export function findMockExpense(id: string): Expense | undefined {
  return mockExpenses.find((e) => e.id === id);
}
