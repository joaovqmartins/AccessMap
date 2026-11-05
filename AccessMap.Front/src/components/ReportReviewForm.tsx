import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { ApiError } from '../api/client';
import { reviewsApi } from '../api/reviews';
import type { ReportReason } from '../api/types';
import { ALL_REPORT_REASONS, REPORT_REASON_LABELS } from '../constants/labels';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';
import { confirm } from '../utils/dialog';
import Button from './Button';
import ErrorMessage from './ErrorMessage';

type Props = {
  reviewId: string;
  onCancel: () => void;
  /** Recebe o aviso a mostrar: denúncia enviada ou já feita antes (409). */
  onDone: (message: string) => void;
};

/** Escolha do motivo e envio da denúncia; quem usa decide onde mostrar o aviso final. */
export default function ReportReviewForm({ reviewId, onCancel, onDone }: Props) {
  const [reason, setReason] = useState<ReportReason | null>(null);
  const [reasonError, setReasonError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  async function handleSubmit() {
    setFormError(null);
    if (!reason) {
      setReasonError('Escolha um motivo');
      return;
    }
    setReasonError(null);

    if (!(await confirm('Denunciar avaliação', 'Tem certeza que quer enviar esta denúncia?', 'Denunciar'))) return;

    setSending(true);
    try {
      await reviewsApi.report(reviewId, reason);
      onDone('Denúncia enviada. Obrigado por avisar.');
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        onDone('Você já denunciou esta avaliação');
      } else if (e instanceof ApiError && e.status === 404) {
        setFormError('Esta avaliação não existe mais.');
      } else {
        setFormError(e instanceof Error ? e.message : 'Não foi possível enviar a denúncia');
      }
    } finally {
      setSending(false);
    }
  }

  return (
    <View>
      <Text style={styles.title} accessibilityRole="header">Por que você quer denunciar?</Text>
      <ErrorMessage message={formError} />

      <View accessibilityRole="radiogroup" accessibilityLabel="Motivo da denúncia">
        {ALL_REPORT_REASONS.map((option) => {
          const selected = reason === option;
          return (
            <Pressable
              key={option}
              onPress={() => setReason(option)}
              accessibilityRole="radio"
              accessibilityLabel={REPORT_REASON_LABELS[option]}
              accessibilityState={{ checked: selected }}
              style={[styles.option, selected && styles.optionSelected]}
            >
              <Text style={[styles.optionText, selected && styles.optionTextSelected]}>
                {selected ? '✓ ' : ''}
                {REPORT_REASON_LABELS[option]}
              </Text>
            </Pressable>
          );
        })}
      </View>
      {!!reasonError && (
        <Text style={styles.error} accessibilityLiveRegion="polite">
          {reasonError}
        </Text>
      )}

      <View style={styles.actions}>
        <Button title="Enviar denúncia" onPress={handleSubmit} loading={sending} />
        <Button title="Cancelar" variant="secondary" onPress={onCancel} disabled={sending} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 17, fontWeight: '700', color: colors.text, marginBottom: spacing.sm },
  option: {
    minHeight: MIN_TOUCH,
    justifyContent: 'center',
    paddingHorizontal: spacing.md,
    marginBottom: spacing.sm,
    borderRadius: 8,
    borderWidth: 1.5,
    borderColor: colors.primary,
    backgroundColor: colors.surface,
  },
  optionSelected: { backgroundColor: colors.primary },
  optionText: { color: colors.primary, fontSize: 15, fontWeight: '600' },
  optionTextSelected: { color: colors.onPrimary },
  error: { color: colors.danger, fontSize: 13, marginBottom: spacing.sm },
  actions: { gap: spacing.sm, marginTop: spacing.sm },
});
