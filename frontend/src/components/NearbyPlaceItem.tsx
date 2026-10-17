import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { PlaceSummary } from '../api/types';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';
import type { NearbyResult } from '../hooks/useNearbyPlaces';
import { formatDistance } from '../utils/distance';
import StarRating from './StarRating';

type Props = {
  place: NearbyResult;
  onPress: () => void;
};

/** Características em que a maioria estrita dos votos é ADEQUADO (mesma regra da busca do backend). */
function adequateTagCount(summary: PlaceSummary) {
  return Object.values(summary.tagStats).filter(
    (s) => s && s.adequadoCount > s.inadequadoCount + s.inexistenteCount,
  ).length;
}

function describe({ name, address, distanceM, summary }: NearbyResult) {
  const parts = [name, `a ${formatDistance(distanceM)}`, address];
  if (summary && summary.reviewCount > 0) {
    const adequate = adequateTagCount(summary);
    parts.push(
      `nota ${summary.averageScore.toFixed(1).replace('.', ',')} de 5`,
      `${summary.reviewCount} ${summary.reviewCount === 1 ? 'avaliação' : 'avaliações'}`,
      `${adequate} ${adequate === 1 ? 'característica adequada' : 'características adequadas'}`,
    );
  } else if (summary) {
    parts.push('sem avaliações de acessibilidade ainda');
  }
  return parts.filter(Boolean).join(', ');
}

export default function NearbyPlaceItem({ place, onPress }: Props) {
  const { name, address, distanceM, summary } = place;
  const rated = !!summary && summary.reviewCount > 0;
  const adequate = rated ? adequateTagCount(summary) : 0;

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={describe(place)}
      accessibilityHint="Abre os detalhes de acessibilidade do local"
      style={({ pressed }) => [styles.card, pressed && styles.pressed]}
    >
      <View style={styles.titleRow}>
        <Text style={styles.name} numberOfLines={1}>{name}</Text>
        <Text style={styles.distance}>{formatDistance(distanceM)}</Text>
      </View>
      {!!address && <Text style={styles.address} numberOfLines={1}>{address}</Text>}

      {rated ? (
        <View style={styles.scoreRow}>
          <StarRating value={summary.averageScore} size={16} />
          <Text style={styles.muted}>
            {summary.averageScore.toFixed(1).replace('.', ',')} · {summary.reviewCount}{' '}
            {summary.reviewCount === 1 ? 'avaliação' : 'avaliações'}
          </Text>
          {adequate > 0 && (
            <Text style={styles.badge}>
              ✓ {adequate} {adequate === 1 ? 'característica adequada' : 'características adequadas'}
            </Text>
          )}
        </View>
      ) : (
        summary && <Text style={styles.unrated}>Sem avaliações de acessibilidade ainda</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    minHeight: MIN_TOUCH,
    backgroundColor: colors.surface,
    borderRadius: 8,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  pressed: { opacity: 0.7 },
  titleRow: { flexDirection: 'row', alignItems: 'baseline' },
  name: { flex: 1, fontSize: 16, fontWeight: '600', color: colors.text, marginRight: spacing.sm },
  distance: { fontSize: 14, color: colors.textMuted },
  address: { fontSize: 14, color: colors.textMuted, marginTop: 2 },
  scoreRow: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', marginTop: spacing.sm, gap: spacing.sm },
  muted: { fontSize: 14, color: colors.textMuted },
  unrated: { fontSize: 14, color: colors.textMuted, marginTop: spacing.sm },
  badge: { fontSize: 13, fontWeight: '600', color: colors.success },
});
