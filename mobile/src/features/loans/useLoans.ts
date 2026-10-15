import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';

import { useMe } from '@/hooks/useMe';
import {
  createLoan,
  getLoan,
  listLoans,
  updateLoan,
  type CreateLoanInput,
  type LoanDto,
  type LoanEmiDto,
  type UpdateLoanInput,
} from '@/services/api/loanApi';
import type { ApiError } from '@/types/api';
import type { LoanDetail, LoanEmi, LoanSummary } from './types';

/**
 * Data hooks for loans (HLD 18, 21). Seam between screens and the API:
 * GET/POST/PUT /api/v1/loans. The backend derives outstanding principal, the
 * end date, and the EMI schedule. Currency is joined from the user profile.
 */

/** Earliest not-yet-paid EMI due date, or null. */
function nextDue(emis: LoanEmiDto[] | undefined): string | null {
  if (!emis) return null;
  const pending = emis
    .filter((e) => e.status !== 'PAID' && e.status !== 'WAIVED')
    .sort((a, b) => (a.dueDate < b.dueDate ? -1 : 1));
  return pending.length > 0 ? pending[0].dueDate : null;
}

function toEmi(e: LoanEmiDto): LoanEmi {
  return {
    id: e.id,
    installmentNo: e.installmentNo,
    dueDate: e.dueDate,
    emiAmount: e.emiAmount,
    principalComponent: e.principalComponent,
    interestComponent: e.interestComponent,
    // The EMI endpoint exposes status, not a paid sum; treat a PAID installment
    // as fully paid and others as unpaid for display.
    paidAmount: e.status === 'PAID' ? e.emiAmount : 0,
    status: e.status,
  };
}

function toSummary(l: LoanDto, currency: string): LoanSummary {
  return {
    id: l.id,
    name: l.name,
    lender: l.lender,
    loanType: l.loanType,
    outstandingPrincipal: l.outstandingPrincipal,
    emiAmount: l.emiAmount,
    nextDueDate: nextDue(l.emis),
    status: l.status,
    currency,
  };
}

function toDetail(l: LoanDto, currency: string): LoanDetail {
  return {
    id: l.id,
    name: l.name,
    lender: l.lender,
    loanType: l.loanType,
    principalAmount: l.principalAmount,
    outstandingPrincipal: l.outstandingPrincipal,
    emiAmount: l.emiAmount,
    interestRate: l.interestRate,
    startDate: l.startDate,
    endDate: l.endDate,
    tenureMonths: l.tenureMonths,
    status: l.status,
    currency,
    emis: (l.emis ?? []).map(toEmi),
  };
}

export type UseLoansResult = {
  data: LoanSummary[] | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useLoans(): UseLoansResult {
  const loans = useQuery({ queryKey: ['loans'], queryFn: listLoans });
  const me = useMe();
  const currency = me.data?.currency ?? 'INR';

  const data = useMemo(
    () => (loans.data ? loans.data.map((l) => toSummary(l, currency)) : null),
    [loans.data, currency],
  );

  return {
    data,
    isLoading: loans.isLoading || me.isLoading,
    isError: loans.isError,
    refetch: () => {
      void loans.refetch();
      void me.refetch();
    },
  };
}

export type UseLoanResult = {
  data: LoanDetail | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useLoan(loanId: string): UseLoanResult {
  const loan = useQuery({ queryKey: ['loans', loanId], queryFn: () => getLoan(loanId), enabled: Boolean(loanId) });
  const me = useMe();
  const currency = me.data?.currency ?? 'INR';

  const data = useMemo(
    () => (loan.data ? toDetail(loan.data, currency) : null),
    [loan.data, currency],
  );

  return {
    data,
    isLoading: loan.isLoading || me.isLoading,
    isError: loan.isError,
    refetch: () => {
      void loan.refetch();
      void me.refetch();
    },
  };
}

/** Create (no id) or update (with id) a loan, then refresh loans + dashboard. */
export function useSaveLoan(loanId?: string) {
  const queryClient = useQueryClient();
  return useMutation<LoanDto, ApiError, CreateLoanInput | UpdateLoanInput>({
    mutationFn: (body) =>
      loanId ? updateLoan(loanId, body as UpdateLoanInput) : createLoan(body as CreateLoanInput),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['loans'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ]);
    },
  });
}
