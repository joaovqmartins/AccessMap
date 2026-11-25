import { View } from 'react-native';
import MapView, { Marker } from 'react-native-maps';

import { LocationFooter, placeMapStyles, type PlaceLocationMapProps } from './PlaceLocationMap.shared';

// ~500 m de altura na tela: dá para reconhecer a rua e o entorno do local
const DELTA = 0.005;

/** Mapa pequeno com o pino do local, no topo da tela de detalhes. */
export default function PlaceLocationMap({ placeId, name, location }: PlaceLocationMapProps) {
  const { latitude, longitude, address } = location;

  return (
    <View style={placeMapStyles.card}>
      <MapView
        style={placeMapStyles.map}
        initialRegion={{ latitude, longitude, latitudeDelta: DELTA, longitudeDelta: DELTA }}
        // Fixo: dentro da tela que rola, arrastar o mapa roubaria o gesto da rolagem.
        // Para explorar, o "Abrir no mapa" leva ao app de mapas.
        scrollEnabled={false}
        zoomEnabled={false}
        rotateEnabled={false}
        pitchEnabled={false}
        toolbarEnabled={false}
        accessibilityLabel={`Mapa com a localização de ${name}${address ? `, ${address}` : ''}`}
      >
        <Marker coordinate={{ latitude, longitude }} title={name} description={address} tracksViewChanges={false} />
      </MapView>
      <LocationFooter placeId={placeId} name={name} location={location} />
    </View>
  );
}
