import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { AccessibilityTag, Review } from '../api/types';
import { ASSESSMENT_LABELS, NEED_LABELS, TAG_LABELS } from '../constants/labels';
import { assessmentColors, colors, spacing } from '../constants/theme';
import StarRating from './StarRating';

type Props = {
  review: Review;
  isMine?: boolean;
  onPress?: () => void;
};

export default function ReviewCard({ review, isMine, onPress }: Props) {
  const tags = Object.entries(review.tags ?? {}) as [AccessibilityTag, keyof typeof ASSESSMENT_LABELS][];
  const date = new Date(review.createdAt).toLocaleDateString('pt-BR');

  return (
    <Pressable
      onPress={onPress}
      disabled={!onPress}
      accessibilityRole={onPress ? 'button' : undefined}
      accessibilityHint={onPress ? 'Abre os detalhes da avaliação' : undefined}
      style={({ pressed }) => [styles.card, isMine && styles.mine, pressed && styles.pressed]}
    >
      <View style={styles.header}>
        <StarRating value={review.rating} size={16} />
        <Text style={styles.author}>{isMine ? 'Sua avaliação' : 'Usuário'} · {date}</Text>
      </View>

      {!!review.comment && <Text style={styles.comment}>{review.comment}</Text>}

      <View style={styles.chips}>
        {tags.map(([tag, assessment]) => (
          <View key={tag} style={[styles.chip, { borderColor: assessmentColors[assessment] }]}>
            <Text style={styles.chipText}>
              {TAG_LABELS[tag]}: <Text style={{ color: assessmentColors[assessment] }}>{ASSESSMENT_LABELS[assessment]}</Text>
            </Text>
          </View>
        ))}
      </View>

      {!!review.reviewerNeeds?.length && (
        <Text style={styles.needs}>
          Avaliado por alguém com: {review.reviewerNeeds.map((n) => NEED_LABELS[n].toLowerCase()).join(', ')}
        </Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  mine: { borderColor: colors.primary, borderWidth: 2 },
  pressed: { opacity: 0.7 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  author: { fontSize: 13, color: colors.textMuted },
  comment: { fontSize: 15, color: colors.text, marginTop: spacing.sm },
  chips: { flexDirection: 'row', flexWrap: 'wrap', marginTop: spacing.sm },
  chip: {
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    marginRight: spacing.xs,
    marginBottom: spacing.xs,
  },
  chipText: { fontSize: 12, color: colors.text },
  needs: { fontSize: 12, color: colors.textMuted, marginTop: spacing.xs, fontStyle: 'italic' },
});
