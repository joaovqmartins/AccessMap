import { useLocalSearchParams } from 'expo-router';

import ReviewDetailScreen from '../../../screens/ReviewDetailScreen';

export default function AvaliacaoDetalhe() {
  const { id, jaAvaliado } = useLocalSearchParams<{ id: string; jaAvaliado?: string }>();
  return <ReviewDetailScreen reviewId={id} alreadyReviewed={jaAvaliado === '1'} />;
}
