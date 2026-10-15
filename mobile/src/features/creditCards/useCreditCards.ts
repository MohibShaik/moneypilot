import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';

import { useMe } from '@/hooks/useMe';
import {
  createCreditCard,
  getCreditCard,
  listCardStatements,
  listCreditCards,
  updateCreditCard,
  type CardStatementDto,
  type CreateCreditCardInput,
  type CreditCardDto,
  type UpdateCreditCardInput,
} from '@/services/api/creditCardApi';
import type { ApiError } from '@/types/api';
import type { CardDetail, CardStatement, CardSummary } from './types';

/**
 * Data hooks for credit cards (HLD 18, 21). Seam between screens and the API.
 * The card endpoints do not expose used/available amounts or a due date; those
 * are derived client-side from the latest statement. The list omits statements,
 * so used/available/dueDate are null there and populate on the detail view.
 */

function toStatement(s: CardStatementDto): CardStatement {
  return {
    id: s.id,
    statementDate: s.statementDate,
    dueDate: s.dueDate,
    statementAmount: s.statementAmount,
    minimumDue: s.minimumDue,
    paidAmount: s.paidAmount,
    status: s.status,
  };
}

function toSummary(c: CreditCardDto, currency: string): CardSummary {
  return {
    id: c.id,
    cardName: c.cardName,
    bank: c.bank,
    lastFour: c.lastFour,
    creditLimit: c.creditLimit,
    availableAmount: null,
    usedAmount: null,
    dueDate: null,
    status: c.status,
    currency,
  };
}

export type UseCreditCardsResult = {
  data: CardSummary[] | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useCreditCards(): UseCreditCardsResult {
  const cards = useQuery({ queryKey: ['credit-cards'], queryFn: listCreditCards });
  const me = useMe();
  const currency = me.data?.currency ?? 'INR';

  const data = useMemo(
    () => (cards.data ? cards.data.map((c) => toSummary(c, currency)) : null),
    [cards.data, currency],
  );

  return {
    data,
    isLoading: cards.isLoading || me.isLoading,
    isError: cards.isError,
    refetch: () => {
      void cards.refetch();
      void me.refetch();
    },
  };
}

export type UseCreditCardResult = {
  data: CardDetail | null;
  isLoading: boolean;
  isError: boolean;
  refetch: () => void;
};

export function useCreditCard(cardId: string): UseCreditCardResult {
  const card = useQuery({ queryKey: ['credit-cards', cardId], queryFn: () => getCreditCard(cardId), enabled: Boolean(cardId) });
  const statements = useQuery({
    queryKey: ['credit-cards', cardId, 'statements'],
    queryFn: () => listCardStatements(cardId),
    enabled: Boolean(cardId),
  });
  const me = useMe();
  const currency = me.data?.currency ?? 'INR';

  const data = useMemo<CardDetail | null>(() => {
    if (!card.data) return null;
    const sorted = [...(statements.data ?? [])]
      .map(toStatement)
      .sort((a, b) => (a.statementDate < b.statementDate ? 1 : -1));
    const current = sorted.length > 0 ? sorted[0] : null;
    // Used = unpaid portion of the latest statement; available = limit - used.
    const used = current ? Math.max(current.statementAmount - current.paidAmount, 0) : 0;
    return {
      id: card.data.id,
      cardName: card.data.cardName,
      bank: card.data.bank,
      lastFour: card.data.lastFour,
      creditLimit: card.data.creditLimit,
      usedAmount: current ? used : null,
      availableAmount: current ? Math.max(card.data.creditLimit - used, 0) : null,
      status: card.data.status,
      currency,
      currentStatement: current,
      statementHistory: sorted.slice(1),
    };
  }, [card.data, statements.data, currency]);

  return {
    data,
    isLoading: card.isLoading || statements.isLoading || me.isLoading,
    isError: card.isError || statements.isError,
    refetch: () => {
      void card.refetch();
      void statements.refetch();
      void me.refetch();
    },
  };
}

/** Create (no id) or update (with id) a card, then refresh cards + dashboard. */
export function useSaveCreditCard(cardId?: string) {
  const queryClient = useQueryClient();
  return useMutation<CreditCardDto, ApiError, CreateCreditCardInput | UpdateCreditCardInput>({
    mutationFn: (body) =>
      cardId ? updateCreditCard(cardId, body as UpdateCreditCardInput) : createCreditCard(body as CreateCreditCardInput),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['credit-cards'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ]);
    },
  });
}
