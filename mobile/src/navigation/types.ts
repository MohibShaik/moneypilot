/**
 * Navigation param lists (HLD §6). Centralized so screens get typed navigation.
 */

export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
};

export type MainTabParamList = {
  Home: undefined;
  Loans: undefined;
  Cards: undefined;
  Expenses: undefined;
  More: undefined;
};

/** Stack nested inside the Loans tab (HLD §6, §11). */
export type LoansStackParamList = {
  LoanList: undefined;
  LoanDetails: { loanId: string };
  /** Add (no id) or edit (with loanId) a loan. */
  LoanForm: { loanId?: string } | undefined;
};

/** Stack nested inside the Cards tab (HLD §6, §12). */
export type CardsStackParamList = {
  CardList: undefined;
  CardDetails: { cardId: string };
  /** Add (no id) or edit (with cardId) a credit card. */
  CardForm: { cardId?: string } | undefined;
};

/** Stack nested inside the Expenses tab (HLD §6, §13). */
export type ExpensesStackParamList = {
  ExpenseList: undefined;
  /** Add (no id) or edit (with expenseId) an expense. */
  ExpenseForm: { expenseId?: string } | undefined;
};

/** Stack nested inside the More tab (HLD §6): Payments, Profile, Settings. */
export type MoreStackParamList = {
  MoreHome: undefined;
  PaymentHistory: undefined;
  PaymentDetails: { paymentId: string };
  /** Pay a loan EMI; `targetId` identifies the payable EMI. */
  LoanEmiPayment: { targetId: string };
  /** Pay a card statement; `targetId` identifies the payable statement. */
  CardPayment: { targetId: string };
  /** Manage the user payment methods. */
  PaymentMethods: undefined;
  Profile: undefined;
  EditProfile: undefined;
  Settings: undefined;
};
