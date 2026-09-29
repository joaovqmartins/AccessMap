import { useLocalSearchParams } from 'expo-router';

import PlaceDetailsScreen from '../../../screens/PlaceDetailsScreen';

export default function LocalDetalhes() {
  const { placeId, name } = useLocalSearchParams<{ placeId: string; name?: string }>();
  return <PlaceDetailsScreen placeId={placeId} name={name} />;
}
