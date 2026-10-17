import { StyleSheet, Text, View } from 'react-native';

import type { AccessibilityTag, TagStats } from '../api/types';
import { TAG_LABELS } from '../constants/labels';
import { assessmentColors, colors, spacing } from '../constants/theme';

type Props = {
  tag: AccessibilityTag;
  stats: TagStats;
};

/** Distribuição adequado/inadequado/inexistente de uma característica, com as contagens em texto. */
export default function TagStatsBar({ tag, stats }: Props) {
  const total = stats.adequadoCount + stats.inadequadoCount + stats.inexistenteCount;
  const segments = [
    { key: 'ADEQUADO', count: stats.adequadoCount, label: 'adequado' },
    { key: 'INADEQUADO', count: stats.inadequadoCount, label: 'inadequado' },
    { key: 'INEXISTENTE', count: stats.inexistenteCount, label: 'inexistente' },
  ] as const;

  const summary = segments.map((s) => `${s.count} ${s.label}`).join(', ');

  return (
    <View style={styles.container} accessible accessibilityLabel={`${TAG_LABELS[tag]}: ${summary}`}>
      <Text style={styles.title}>{TAG_LABELS[tag]}</Text>
      <View style={styles.bar}>
        {total > 0 &&
          segments.map((s) =>
            s.count > 0 ? (
              <View key={s.key} style={{ flex: s.count, backgroundColor: assessmentColors[s.key] }} />
            ) : null,
          )}
      </View>
      <View style={styles.legend}>
        {segments.map((s) => (
          <View key={s.key} style={styles.legendItem}>
            <View style={[styles.dot, { backgroundColor: assessmentColors[s.key] }]} />
            <Text style={styles.legendText}>
              {s.count} {s.label}
            </Text>
          </View>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { marginBottom: spacing.md },
  title: { fontSize: 15, fontWeight: '600', color: colors.text, marginBottom: spacing.xs },
  bar: {
    flexDirection: 'row',
    height: 10,
    borderRadius: 5,
    overflow: 'hidden',
    backgroundColor: colors.border,
  },
  legend: { flexDirection: 'row', flexWrap: 'wrap', marginTop: spacing.xs },
  legendItem: { flexDirection: 'row', alignItems: 'center', marginRight: spacing.md },
  dot: { width: 8, height: 8, borderRadius: 4, marginRight: spacing.xs },
  legendText: { fontSize: 13, color: colors.textMuted },
});
