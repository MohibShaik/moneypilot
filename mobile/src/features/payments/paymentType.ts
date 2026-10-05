import type { PaymentType } from './types';

/** Human-readable label for a payment type (HLD §14). */
export function paymentTypeLabel(type: PaymentType): string {
  switch (type) {
    case 'LOAN_EMI':
      return 'Loan EMI';
    case 'CREDIT_CARD':
      return 'Credit Card Payment';
    case 'OTHER':
      return 'Other Payment';
  }
}

/** Ionicons glyph name for a payment type (used with @expo/vector-icons). */
export function paymentTypeIcon(type: PaymentType): 'cash-outline' | 'card-outline' | 'ellipsis-horizontal' {
  switch (type) {
    case 'LOAN_EMI':
      return 'cash-outline';
    case 'CREDIT_CARD':
      return 'card-outline';
    case 'OTHER':
      return 'ellipsis-horizontal';
  }
}
