import { http } from './client';

/**
 * Loan API (services/api layer). Mirrors the backend LoanResponse /
 * LoanEmiResponse DTOs and the Create/Update request shapes. The shared client
 * unwraps the envelope and attaches the Bearer token.
 */

export type LoanType = 'HOME' | 'CAR' | 'PERSONAL' | 'EDUCATION' | 'GOLD' | 'OTHER';
export type LoanStatus = 'ACTIVE' | 'CLOSED' | 'FORECLOSED';
export type EmiStatus = 'UPCOMING' | 'DUE' | 'PAID' | 'OVERDUE' | 'WAIVED';

export type LoanEmiDto = {
  id: string;
  installmentNo: number;
  principalComponent: number;
  interestComponent: number;
  emiAmount: number;
  dueDate: string;
  paidDate: string | null;
  status: EmiStatus;
};

/** emis is present only on the detail endpoint (omitted from the list). */
export type LoanDto = {
  id: string;
  name: string;
  lender: string | null;
  loanType: LoanType;
  principalAmount: number;
  outstandingPrincipal: number;
  emiAmount: number;
  interestRate: number;
  startDate: string;
  endDate: string | null;
  tenureMonths: number;
  status: LoanStatus;
  createdAt: string;
  emis?: LoanEmiDto[];
};

/** Create body. outstandingPrincipal, endDate and the EMI schedule are
 * derived server-side; emiAmount is optional (computed when omitted). */
export type CreateLoanInput = {
  name: string;
  lender?: string;
  loanType: LoanType;
  principalAmount: number;
  interestRate: number;
  startDate: string;
  tenureMonths: number;
  emiAmount?: number;
};

/** Update body. Financial terms are immutable; only these are editable. */
export type UpdateLoanInput = {
  name: string;
  lender?: string;
  status: LoanStatus;
};

/** GET /loans (summaries, no emis). */
export async function listLoans(): Promise<LoanDto[]> {
  const res = await http.get<LoanDto[]>('/loans');
  return res.data;
}

/** GET /loans/{id} (detail incl. emis). */
export async function getLoan(id: string): Promise<LoanDto> {
  const res = await http.get<LoanDto>(`/loans/${id}`);
  return res.data;
}

/** POST /loans */
export async function createLoan(body: CreateLoanInput): Promise<LoanDto> {
  const res = await http.post<LoanDto>('/loans', body);
  return res.data;
}

/** PUT /loans/{id} */
export async function updateLoan(id: string, body: UpdateLoanInput): Promise<LoanDto> {
  const res = await http.put<LoanDto>(`/loans/${id}`, body);
  return res.data;
}
