import { StyleSheet, Text, View } from 'react-native';

import type { AccessibilityTag, AdminReview, TagAssessment } from '../api/types';
import { ASSESSMENT_LABELS, MODERATION_STATUS_LABELS, SUSPICION_REASON_LABELS, TAG_LABELS } from '../constants/labels';
import { assessmentColors, colors, spacing } from '../constants/theme';
import Button from './Button';
import StarRating from './StarRating';

export type ModerationAction = 'restore' | 'keep' | 'remove';

type Props = {
  review: AdminReview;
  busy: ModerationAction | null;
  disabled: boolean;
  onAction: (action: ModerationAction) => void;
};

/** Item da fila: a situação e o motivo vêm em texto, para o ADMIN decidir sem depender de cor. */
export default function ModerationCard({ review, busy, disabled, onAction }: Props) {
  const tags = Object.entries(review.tags ?? {}) as [AccessibilityTag, TagAssessment][];
  const hidden = review.status === 'OCULTA';
  const reason = SUSPICION_REASON_LABELS[review.suspicionReason];
  const date = new Date(review.createdAt).toLocaleDateString('pt-BR');

  return (
    <View style={styles.card}>
      <Text style={styles.status}>
        {MODERATION_STATUS_LABELS[review.status]} · {review.reportCount > 0 ? `${reason} (${review.reportCount})` : reason}
      </Text>

      <View style={styles.header}>
        <StarRating value={review.rating} size={16} />
        <Text style={styles.date}>{date}</Text>
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

      <View style={styles.actions}>
        {hidden ? (
          <Button
            title="Restaurar"
            accessibilityHint="Volta a avaliação para o ar e para a nota do local"
            onPress={() => onAction('restore')}
            loading={busy === 'restore'}
            disabled={disabled}
          />
        ) : (
          <Button
            title="Manter"
            accessibilityHint="Tira da fila e mantém a avaliação no ar"
            onPress={() => onAction('keep')}
            loading={busy === 'keep'}
            disabled={disabled}
          />
        )}
        <Button
          title="Remover"
          variant="danger"
          accessibilityHint="Tira a avaliação do ar e conta contra o autor"
          onPress={() => onAction('remove')}
          loading={busy === 'remove'}
          disabled={disabled}
        />
      </View>
    </View>
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
  status: { fontSize: 14, fontWeight: '700', color: colors.text, marginBottom: spacing.sm },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  date: { fontSize: 13, color: colors.textMuted },
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
  actions: { gap: spacing.sm, marginTop: spacing.sm },
});
