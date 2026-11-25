import { useLocalSearchParams } from 'expo-router';

import ReviewFormScreen from '../../../screens/ReviewFormScreen';

export default function LocalAvaliar() {
  const { placeId, name } = useLocalSearchParams<{ placeId: string; name?: string }>();
  return <ReviewFormScreen mode="create" placeId={placeId} placeName={name} />;
}
