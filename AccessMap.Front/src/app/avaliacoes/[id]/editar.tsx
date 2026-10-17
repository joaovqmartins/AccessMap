import { useLocalSearchParams } from 'expo-router';

import ReviewFormScreen from '../../../screens/ReviewFormScreen';

export default function AvaliacaoEditar() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <ReviewFormScreen mode="edit" reviewId={id} />;
}
