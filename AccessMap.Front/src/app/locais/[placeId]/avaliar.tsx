import { useLocalSearchParams } from 'expo-router';

import ReviewFormScreen from '../../../screens/ReviewFormScreen';

export default function LocalAvaliar() {
  const { placeId } = useLocalSearchParams<{ placeId: string }>();
  return <ReviewFormScreen mode="create" placeId={placeId} />;
}
