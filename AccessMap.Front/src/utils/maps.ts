import { Platform } from 'react-native';

import type { PlaceLocation } from '../api/googlePlaces';

/**
 * Endereço que abre o local no app de mapas: Apple Maps no iPhone, Google Maps no resto.
 * Não usa a chave do Google: são links públicos.
 */
export function externalMapUrl(placeId: string, name: string, location: PlaceLocation) {
  const coords = `${location.latitude},${location.longitude}`;
  if (Platform.OS === 'ios') {
    return `http://maps.apple.com/?ll=${coords}&q=${encodeURIComponent(name || location.address)}`;
  }
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(coords)}&query_place_id=${encodeURIComponent(placeId)}`;
}
