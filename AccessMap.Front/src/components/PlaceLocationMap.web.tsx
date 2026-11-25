import { View } from 'react-native';

import { LocationFooter, placeMapStyles, type PlaceLocationMapProps } from './PlaceLocationMap.shared';

/**
 * O react-native-maps não roda no navegador: na web a tela do local mostra só o endereço e o link
 * para abrir no Google Maps. Arquivo separado para o mapa nativo nem entrar no bundle web.
 */
export default function PlaceLocationMap(props: PlaceLocationMapProps) {
  return (
    <View style={placeMapStyles.card}>
      <LocationFooter {...props} />
    </View>
  );
}
