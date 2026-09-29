import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { AccessibilityNeed } from '../api/types';
import { ALL_NEEDS, NEED_LABELS } from '../constants/labels';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';

type Props = {
  value: AccessibilityNeed[];
  onChange: (value: AccessibilityNeed[]) => void;
  error?: string;
};

/** Seleção múltipla das necessidades de acessibilidade. */
export default function NeedsSelector({ value, onChange, error }: Props) {
  function toggle(need: AccessibilityNeed) {
    onChange(value.includes(need) ? value.filter((n) => n !== need) : [...value, need]);
  }

  return (
    <View style={styles.container}>
      <Text style={styles.label}>Necessidades de acessibilidade</Text>
      <View style={styles.chips}>
        {ALL_NEEDS.map((need) => {
          const selected = value.includes(need);
          return (
            <Pressable
              key={need}
              onPress={() => toggle(need)}
              accessibilityRole="checkbox"
              accessibilityLabel={NEED_LABELS[need]}
              accessibilityState={{ checked: selected }}
              style={[styles.chip, selected && styles.chipSelected]}
            >
              <Text style={[styles.chipText, selected && styles.chipTextSelected]}>
                {selected ? '✓ ' : ''}
                {NEED_LABELS[need]}
              </Text>
            </Pressable>
          );
        })}
      </View>
      {!!error && (
        <Text style={styles.error} accessibilityLiveRegion="polite">
          {error}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { marginBottom: spacing.md },
  label: { fontSize: 14, fontWeight: '600', color: colors.text, marginBottom: spacing.xs },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  chip: {
    minHeight: MIN_TOUCH,
    justifyContent: 'center',
    paddingHorizontal: spacing.md,
    borderRadius: MIN_TOUCH / 2,
    borderWidth: 1.5,
    borderColor: colors.primary,
    backgroundColor: colors.surface,
  },
  chipSelected: { backgroundColor: colors.primary },
  chipText: { color: colors.primary, fontWeight: '600', fontSize: 14 },
  chipTextSelected: { color: colors.onPrimary },
  error: { color: colors.danger, fontSize: 13, marginTop: spacing.xs },
});
