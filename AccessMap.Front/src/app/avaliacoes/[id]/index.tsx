import { useLocalSearchParams } from 'expo-router';

import ReviewDetailScreen from '../../../screens/ReviewDetailScreen';

export default function AvaliacaoDetalhe() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <ReviewDetailScreen reviewId={id} />;
}
