import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import { useMe } from '@/hooks/useMe';
import { listCardStatements, listCreditCards } from '@/services/api/creditCardApi';
import { getLoan, listLoans } from '@/services/api/loanApi';
import {
  getPayment,
  listPayments,
  type PaymentDto,
} from '@/services/api/paymentApi';
import type { ApiError } from '@/types/api';
import { paymentMethodsQuery } from '@/features/expenses/useLookups';
import { paymentTypeLabel } from './paymentType';
import type { PayableTarget, Payment, PaymentGroup } from './types';

/**
 * Data hooks for payments (HLD §18, §21). The seam between screens and the API:
 * GET/POST/DELETE /api/v1/payments, plus loan/card lookups for the payable
 * target. The backend PaymentResponse is flat (type + raw target ids + a full
 * ISO timestamp) with no human-readable label, payment-method label, or
 * currency, so this hook adapts it into the mobile `Payment` view type:
 * - `paidAt` is reduced to a YYYY-MM-DD date (matching the view contract);
 * - `label` is derived from the payment type (the backend exposes no title);
 * - `paymentMethodLabel` is resolved from the payment-methods lookup;
 * - `currency` comes from the user's profile (GET /users/me), the same source
 *   the expenses and dashboard screens use.
 * All amounts stay backend-computed; nothing authoritative is calculated here
 * (HLD §4.1).
 */

/** Reduces a full ISO timestamp to its YYYY-MM-DD date (the view contract). */
function toIsoDate(isoTimestamp: string): string {
  return isoTimestamp.slice(0, 10);
}

function toPayment(dto: PaymentDto, methodLabel: string | null, currency: string): Payment {
  return {
    id: dto.id,
    paymentType: dto.type,
    // The backend returns no human-readable title, so label the payment by its
    // type (e.g. "Loan EMI", "Credit Card Payment", "Other Payment").
    label: paymentTypeLabel(dto.type),
    amount: dto.amount,
    paidAt: toIsoDate(dto.paidAt),
    paymentMethodLabel: methodLabel,
    currency,
  };
}

function groupByDate(payments: Payment[]): PaymentGroup[] {
  const byDate = new Map<string, Payment[]>();
  for (const p of payments) {
    const bucket = byDate.get(p.paidAt) ?? [];
    bucket.push(p);
    byDate.set(p.paidAt, bucket);
  }
  return [...byDate.entries()]
    .sort(([a], [b]) => (a < b ? 1 : -1))
    .map(([date, items]) => ({ date, payments: items }));
}

export type UsePaymentsResult = {
  data: PaymentGroup[] | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function usePayments(): UsePaymentsResult {
  const payments = useQuery({ queryKey: ['payments'], queryFn: () => listPayments() });
  const methods = useQuery(paymentMethodsQuery);
  const me = useMe();

  const data = useMemo(() => {
    if (!payments.data) return null;
    const methodLabels = new Map((methods.data ?? []).map((p) => [p.id, p.label] as const));
    const currency = me.data?.currency ?? 'INR';
    const rows = payments.data.map((dto) =>
      toPayment(dto, dto.paymentMethodId ? methodLabels.get(dto.paymentMethodId) ?? null : null, currency),
    );
    return groupByDate(rows);
  }, [payments.data, methods.data, me.data]);

  return {
    data,
    isLoading: payments.isLoading || methods.isLoading || me.isLoading,
    isError: payments.isError || methods.isError || me.isError,
    refetch: () => {
      void payments.refetch();
      void methods.refetch();
      void me.refetch();
    },
  };
}

export type UsePaymentResult = {
  data: Payment | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function usePayment(paymentId: string): UsePaymentResult {
  const payment = useQuery<PaymentDto, ApiError>({
    queryKey: ['payments', paymentId],
    queryFn: () => getPayment(paymentId),
    enabled: Boolean(paymentId),
  });
  const methods = useQuery(paymentMethodsQuery);
  const me = useMe();

  const data = useMemo(() => {
    if (!payment.data) return null;
    const methodLabel = payment.data.paymentMethodId
      ? (methods.data ?? []).find((p) => p.id === payment.data!.paymentMethodId)?.label ?? null
      : null;
    const currency = me.data?.currency ?? 'INR';
    return toPayment(payment.data, methodLabel, currency);
  }, [payment.data, methods.data, me.data]);

  return {
    data,
    isLoading: payment.isLoading || methods.isLoading || me.isLoading,
    isError: payment.isError || methods.isError || me.isError,
    refetch: () => {
      void payment.refetch();
      void methods.refetch();
      void me.refetch();
    },
  };
}

export type UsePayableTargetResult = {
  data: PayableTarget | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

/**
 * Resolves a payable target (loan EMI or card statement) by id for a payment
 * form, with its authoritative amounts.
 *
 * The backend has no "payable target" endpoint and no fetch-by-id for an EMI
 * or a statement — EMIs are nested in a loan's detail and statements are nested
 * under a card. The two payment screens forward only a bare `targetId`, so this
 * resolves it by scanning the user's loans (EMI schedules) and cards'
 * statements for a matching id. Both monetary fields come from the backend:
 * - EMI: `emiAmount` is the total; an EMI is binary (PAID or not), so
 *   `alreadyPaid`/`remaining` reflect `emiAmount` vs 0 by its PAID status.
 * - Statement: `statementAmount` is the total and `paidAmount` the paid part;
 *   `remaining` is their difference (presentation-level subtraction of two
 *   backend-authoritative fields, not a client-invented amount — HLD §4.1).
 */
async function resolvePayableTarget(targetId: string, currency: string): Promise<PayableTarget> {
  const [loans, cards] = await Promise.all([listLoans(), listCreditCards()]);

  // Loan EMIs: the schedule is only on the loan detail, so fetch each loan.
  const loanDetails = await Promise.all(loans.map((l) => getLoan(l.id)));
  for (const loan of loanDetails) {
    const emi = loan.emis?.find((e) => e.id === targetId);
    if (emi) {
      const paid = emi.status === 'PAID';
      return {
        id: emi.id,
        kind: 'LOAN_EMI',
        title: `${loan.name} · EMI #${emi.installmentNo}`,
        totalAmount: emi.emiAmount,
        alreadyPaid: paid ? emi.emiAmount : 0,
        remaining: paid ? 0 : emi.emiAmount,
        currency,
      };
    }
  }

  // Card statements: nested per card, so scan each card's statements.
  const statementsByCard = await Promise.all(cards.map((c) => listCardStatements(c.id)));
  for (let i = 0; i < cards.length; i += 1) {
    const card = cards[i];
    const statement = statementsByCard[i].find((s) => s.id === targetId);
    if (statement) {
      return {
        id: statement.id,
        kind: 'CREDIT_CARD',
        title: `${card.cardName} · Statement`,
        totalAmount: statement.statementAmount,
        alreadyPaid: statement.paidAmount,
        remaining: statement.statementAmount - statement.paidAmount,
        currency,
      };
    }
  }

  throw { code: 'NOT_FOUND', message: 'Payable target not found.' } satisfies ApiError;
}

/**
 * Loads a payable target (loan EMI or card statement) with its authoritative
 * remaining amount for a payment form. Currency comes from the user's profile
 * (GET /users/me), the same source the other screens use.
 */
export function usePayableTarget(targetId: string): UsePayableTargetResult {
  const me = useMe();
  const currency = me.data?.currency ?? 'INR';
  const query = useQuery<PayableTarget, ApiError>({
    queryKey: ['payable-target', targetId, currency],
    queryFn: () => resolvePayableTarget(targetId, currency),
    enabled: Boolean(targetId),
  });

  return {
    data: query.data ?? null,
    isLoading: query.isLoading || me.isLoading,
    isError: query.isError || me.isError,
    refetch: () => {
      void query.refetch();
      void me.refetch();
    },
  };
}
