import type { NativeStackScreenProps } from '@react-navigation/native-stack';

import type { MoreStackParamList } from '@/navigation/types';
import { PaymentForm } from './PaymentForm';

type Props = NativeStackScreenProps<MoreStackParamList, 'LoanEmiPayment'>;

/**
 * Make a loan EMI payment (HLD §14). Shows the EMI's authoritative total /
 * paid / remaining and records a (partial-capable) payment. UI-first placeholder
 * submit; the backend determines the authoritative remaining.
 */
export function LoanEmiPaymentScreen({ route, navigation }: Props) {
  return <PaymentForm targetId={route.params.targetId} onDone={() => navigation.goBack()} />;
}
