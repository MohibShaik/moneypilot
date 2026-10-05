import type { LoanDetail, LoanEmi, LoanSummary } from '../types';

/**
 * Mock loan data for UI-first development (HLD §20). Isolated so it can be
 * replaced by `/api/v1/loans` responses without touching the screens. Values
 * resemble the eventual API DTO and are illustrative only (never computed on
 * the client, HLD §4.1).
 */

export const mockLoans: LoanSummary[] = [
  {
    id: 'loan-1',
    name: 'Home Loan',
    lender: 'SBI',
    loanType: 'HOME',
    outstandingPrincipal: 3250000,
    emiAmount: 28500,
    nextDueDate: '2026-10-10',
    status: 'ACTIVE',
    currency: 'INR',
  },
  {
    id: 'loan-2',
    name: 'Car Loan',
    lender: 'HDFC Bank',
    loanType: 'CAR',
    outstandingPrincipal: 420000,
    emiAmount: 15200,
    nextDueDate: '2026-10-18',
    status: 'ACTIVE',
    currency: 'INR',
  },
];

function buildSchedule(): LoanEmi[] {
  return [
    {
      id: 'emi-1',
      installmentNo: 1,
      dueDate: '2026-08-10',
      emiAmount: 28500,
      principalComponent: 6000,
      interestComponent: 22500,
      paidAmount: 28500,
      status: 'PAID',
    },
    {
      id: 'emi-2',
      installmentNo: 2,
      dueDate: '2026-09-10',
      emiAmount: 28500,
      principalComponent: 6050,
      interestComponent: 22450,
      paidAmount: 28500,
      status: 'PAID',
    },
    {
      id: 'emi-3',
      installmentNo: 3,
      dueDate: '2026-10-10',
      emiAmount: 28500,
      principalComponent: 6100,
      interestComponent: 22400,
      paidAmount: 10000,
      status: 'DUE',
    },
    {
      id: 'emi-4',
      installmentNo: 4,
      dueDate: '2026-11-10',
      emiAmount: 28500,
      principalComponent: 6150,
      interestComponent: 22350,
      paidAmount: 0,
      status: 'UPCOMING',
    },
  ];
}

const loanDetails: Record<string, LoanDetail> = {
  'loan-1': {
    id: 'loan-1',
    name: 'Home Loan',
    lender: 'SBI',
    loanType: 'HOME',
    principalAmount: 3500000,
    outstandingPrincipal: 3250000,
    emiAmount: 28500,
    interestRate: 8.35,
    startDate: '2026-08-10',
    endDate: '2046-08-10',
    tenureMonths: 240,
    status: 'ACTIVE',
    currency: 'INR',
    emis: buildSchedule(),
  },
  'loan-2': {
    id: 'loan-2',
    name: 'Car Loan',
    lender: 'HDFC Bank',
    loanType: 'CAR',
    principalAmount: 600000,
    outstandingPrincipal: 420000,
    emiAmount: 15200,
    interestRate: 9.1,
    startDate: '2025-11-18',
    endDate: '2029-11-18',
    tenureMonths: 48,
    status: 'ACTIVE',
    currency: 'INR',
    emis: buildSchedule().map((e) => ({ ...e, emiAmount: 15200 })),
  },
};

export function findMockLoanDetail(id: string): LoanDetail | undefined {
  return loanDetails[id];
}
