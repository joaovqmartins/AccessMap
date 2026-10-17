import { StyleSheet, Text, View } from 'react-native';
import MapView, { Marker } from 'react-native-maps';

import { colors, spacing } from '../constants/theme';
import type { NearbyResult } from '../hooks/useNearbyPlaces';
import type { Coords } from '../hooks/useUserLocation';
import { formatDistance } from '../utils/distance';
import { adequateTagCount } from '../utils/placeSummary';

export type PlacesMapProps = {
  center: Coords;
  places: NearbyResult[];
  /** Locais sem avaliação que ficaram de fora do filtro: aparecem com a cor neutra. */
  unrated: NearbyResult[];
  onOpen: (place: NearbyResult) => void;
};

// Cores de pino que o Android aceita (ele só conhece uma lista fixa de nomes).
// `swatch` é o tom do pino nativo, para a legenda bater com o mapa.
// A cor nunca vem sozinha: a legenda e o balão do marcador dizem o mesmo em texto.
const PIN = {
  adequate: { color: 'green', swatch: '#2E9E45', label: 'Com característica adequada' },
  rated: { color: 'blue', swatch: '#2F6FE0', label: 'Avaliado' },
  unrated: { color: 'tan', swatch: '#C9A97A', label: 'Sem avaliação' },
} as const;

type PinKind = keyof typeof PIN;

function kindOf({ summary }: NearbyResult): PinKind {
  if (!summary || summary.reviewCount === 0) return 'unrated';
  return adequateTagCount(summary) > 0 ? 'adequate' : 'rated';
}

function describe(place: NearbyResult) {
  const { summary, distanceM } = place;
  const parts = [formatDistance(distanceM)];
  if (summary && summary.reviewCount > 0) {
    const adequate = adequateTagCount(summary);
    parts.push(
      `nota ${summary.averageScore.toFixed(1).replace('.', ',')}`,
      `${summary.reviewCount} ${summary.reviewCount === 1 ? 'avaliação' : 'avaliações'}`,
    );
    if (adequate > 0) parts.push(`${adequate} ${adequate === 1 ? 'característica adequada' : 'características adequadas'}`);
  } else if (summary) {
    parts.push('sem avaliações ainda');
  }
  return `${parts.join(' · ')}. Toque para ver detalhes.`;
}

// ~2 km de altura na tela: mostra os mais próximos sem amontoar os pinos
const DELTA = 0.02;

export default function PlacesMap({ center, places, unrated, onOpen }: PlacesMapProps) {
  return (
    <View style={styles.container}>
      <MapView
        style={StyleSheet.absoluteFill}
        initialRegion={{ ...center, latitudeDelta: DELTA, longitudeDelta: DELTA }}
        showsUserLocation
        showsMyLocationButton
        accessibilityLabel={`Mapa com ${places.length + unrated.length} locais. A visualização em lista traz as mesmas informações.`}
      >
        {[...places, ...unrated].map((p) => (
          <Marker
            key={p.placeId}
            coordinate={{ latitude: p.latitude, longitude: p.longitude }}
            title={p.name}
            description={describe(p)}
            pinColor={PIN[kindOf(p)].color}
            onCalloutPress={() => onOpen(p)}
            // Pino padrão não muda depois de desenhado: evita redesenhar a cada frame
            tracksViewChanges={false}
          />
        ))}
      </MapView>

      <View style={styles.legend} accessibilityRole="summary">
        {(Object.keys(PIN) as PinKind[]).map((kind) => (
          <View key={kind} style={styles.legendRow}>
            <View style={[styles.dot, { backgroundColor: PIN[kind].swatch }]} />
            <Text style={styles.legendText}>{PIN[kind].label}</Text>
          </View>
        ))}
        {/* Atribuição dos dados do Google Places (no iOS o mapa é da Apple) */}
        <Text style={styles.attribution}>Locais: Google Maps</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  legend: {
    position: 'absolute',
    left: spacing.sm,
    bottom: spacing.sm,
    backgroundColor: 'rgba(255,255,255,0.95)',
    borderRadius: 8,
    padding: spacing.sm,
    gap: 2,
  },
  legendRow: { flexDirection: 'row', alignItems: 'center' },
  dot: { width: 12, height: 12, borderRadius: 6, marginRight: spacing.xs },
  legendText: { fontSize: 13, color: colors.text },
  attribution: { fontSize: 11, color: colors.textMuted, marginTop: 2 },
});
