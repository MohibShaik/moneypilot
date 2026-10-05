/**
 * Loan DTO types (HLD §11, §20). Shaped to resemble the future `/api/v1/loans`
 * responses and aligned with the backend V2 schema (loan_type, status, EMI
 * components). All monetary values are backend-computed; the client never
 * calculates authoritative loan state (HLD §4.1).
 */

export type LoanType = 'HOME' | 'CAR' | 'PERSONAL' | 'EDUCATION' | 'GOLD' | 'OTHER';

export type LoanStatus = 'ACTIVE' | 'CLOSED' | 'FORECLOSED';

export type EmiStatus = 'UPCOMING' | 'DUE' | 'PAID' | 'OVERDUE' | 'WAIVED';

/** Row shape for the loan list (HLD §11 "Loan List"). */
export type LoanSummary = {
  id: string;
  name: string;
  lender: string | null;
  loanType: LoanType;
  outstandingPrincipal: number;
  emiAmount: number;
  /** ISO date (YYYY-MM-DD) of the next due EMI, or null if none remain. */
  nextDueDate: string | null;
  status: LoanStatus;
  currency: string;
};

/** A single installment in the EMI schedule (HLD §11 "EMI Schedule"). */
export type LoanEmi = {
  id: string;
  installmentNo: number;
  /** ISO date (YYYY-MM-DD). */
  dueDate: string;
  emiAmount: number;
  principalComponent: number;
  interestComponent: number;
  /** Backend-computed amount paid against this EMI (partial payments allowed). */
  paidAmount: number;
  status: EmiStatus;
};

/** Full loan detail (HLD §11 "Loan Details"). */
export type LoanDetail = {
  id: string;
  name: string;
  lender: string | null;
  loanType: LoanType;
  principalAmount: number;
  outstandingPrincipal: number;
  emiAmount: number;
  interestRate: number;
  /** ISO dates (YYYY-MM-DD). */
  startDate: string;
  endDate: string | null;
  tenureMonths: number;
  status: LoanStatus;
  currency: string;
  emis: LoanEmi[];
};
