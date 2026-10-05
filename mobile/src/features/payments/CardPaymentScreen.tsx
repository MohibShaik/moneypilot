import type { NativeStackScreenProps } from '@react-navigation/native-stack';

import type { MoreStackParamList } from '@/navigation/types';
import { PaymentForm } from './PaymentForm';

type Props = NativeStackScreenProps<MoreStackParamList, 'CardPayment'>;

/**
 * Make a credit-card statement payment (HLD §14). Shows the statement's
 * authoritative total / paid / remaining and records a (partial-capable)
 * payment. UI-first placeholder submit; the backend is authoritative.
 */
export function CardPaymentScreen({ route, navigation }: Props) {
  return <PaymentForm targetId={route.params.targetId} onDone={() => navigation.goBack()} />;
}
