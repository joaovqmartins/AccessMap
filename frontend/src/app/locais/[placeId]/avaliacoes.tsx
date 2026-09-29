import { useLocalSearchParams } from 'expo-router';

import ReviewListScreen from '../../../screens/ReviewListScreen';

export default function LocalAvaliacoes() {
  const { placeId } = useLocalSearchParams<{ placeId: string }>();
  return <ReviewListScreen placeId={placeId} />;
}
