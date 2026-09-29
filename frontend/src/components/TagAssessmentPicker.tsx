import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { AccessibilityTag, ReviewTags, TagAssessment } from '../api/types';
import { ALL_ASSESSMENTS, ALL_TAGS, ASSESSMENT_LABELS, TAG_LABELS } from '../constants/labels';
import { MIN_TOUCH, assessmentColors, colors, spacing } from '../constants/theme';

type Props = {
  value: ReviewTags;
  onChange: (value: ReviewTags) => void;
  error?: string;
};

/**
 * Uma linha por característica, com Adequado/Inadequado/Inexistente.
 * Tocar de novo na opção marcada desmarca: a característica fica "não avaliada" e não é enviada.
 */
export default function TagAssessmentPicker({ value, onChange, error }: Props) {
  function toggle(tag: AccessibilityTag, assessment: TagAssessment) {
    const next = { ...value };
    if (next[tag] === assessment) delete next[tag];
    else next[tag] = assessment;
    onChange(next);
  }

  return (
    <View>
      {ALL_TAGS.map((tag) => (
        <View key={tag} style={styles.row} accessibilityRole="radiogroup" accessibilityLabel={TAG_LABELS[tag]}>
          <Text style={styles.tag}>{TAG_LABELS[tag]}</Text>
          <View style={styles.options}>
            {ALL_ASSESSMENTS.map((assessment) => {
              const selected = value[tag] === assessment;
              const color = assessmentColors[assessment];
              return (
                <Pressable
                  key={assessment}
                  onPress={() => toggle(tag, assessment)}
                  accessibilityRole="radio"
                  accessibilityLabel={`${TAG_LABELS[tag]}: ${ASSESSMENT_LABELS[assessment]}`}
                  accessibilityState={{ checked: selected }}
                  style={[styles.option, { borderColor: color }, selected && { backgroundColor: color }]}
                >
                  <Text style={[styles.optionText, { color: selected ? colors.onPrimary : color }]}>
                    {selected ? '✓ ' : ''}
                    {ASSESSMENT_LABELS[assessment]}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </View>
      ))}
      {!!error && (
        <Text style={styles.error} accessibilityLiveRegion="polite">
          {error}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { marginBottom: spacing.md },
  tag: { fontSize: 15, fontWeight: '600', color: colors.text, marginBottom: spacing.xs },
  options: { flexDirection: 'row', gap: spacing.xs },
  option: {
    flex: 1,
    minHeight: MIN_TOUCH,
    borderWidth: 1.5,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: spacing.xs,
  },
  optionText: { fontSize: 13, fontWeight: '600' },
  error: { color: colors.danger, fontSize: 13 },
});
