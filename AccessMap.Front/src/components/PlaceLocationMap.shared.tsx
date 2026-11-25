import { Linking, Pressable, StyleSheet, Text, View } from 'react-native';

import type { PlaceLocation } from '../api/googlePlaces';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';
import { externalMapUrl } from '../utils/maps';

// Parte comum do mapa do local: fica fora do PlaceLocationMap.tsx porque aquele importa o
// react-native-maps, que não pode entrar no bundle web.

export type PlaceLocationMapProps = {
  placeId: string;
  name: string;
  location: PlaceLocation;
};

export const PLACE_MAP_HEIGHT = 180;

/** Endereço e botão para abrir no app de mapas, embaixo do mapa (ou sozinho, na web). */
export function LocationFooter({ placeId, name, location }: PlaceLocationMapProps) {
  return (
    <View style={placeMapStyles.footer}>
      {!!location.address && <Text style={placeMapStyles.address}>{location.address}</Text>}
      <Pressable
        onPress={() => Linking.openURL(externalMapUrl(placeId, name, location))}
        accessibilityRole="link"
        accessibilityLabel="Abrir no mapa"
        accessibilityHint="Sai do AccessMap e mostra o local no app de mapas"
        style={placeMapStyles.openButton}
        hitSlop={4}
      >
        <Text style={placeMapStyles.openText}>Abrir no mapa</Text>
      </Pressable>
    </View>
  );
}

export const placeMapStyles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: 8,
    overflow: 'hidden',
    marginBottom: spacing.md,
  },
  map: { height: PLACE_MAP_HEIGHT, width: '100%' },
  placeholder: { height: PLACE_MAP_HEIGHT, backgroundColor: colors.border },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
  },
  address: { flex: 1, fontSize: 14, color: colors.textMuted },
  openButton: { minHeight: MIN_TOUCH, justifyContent: 'center' },
  openText: { fontSize: 15, fontWeight: '600', color: colors.primary },
});
