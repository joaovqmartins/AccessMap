import { Stack, router } from 'expo-router';
import { useEffect, useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';

import { ApiError } from '../api/client';
import { reviewsApi } from '../api/reviews';
import type { Review, ReviewTags, ReviewUpdateRequest } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import StarRatingInput from '../components/StarRatingInput';
import TagAssessmentPicker from '../components/TagAssessmentPicker';
import TextField from '../components/TextField';
import { colors, spacing } from '../constants/theme';

type Props =
  | { mode: 'create'; placeId: string }
  | { mode: 'edit'; reviewId: string };

type Errors = { rating?: string; tags?: string };

export default function ReviewFormScreen(props: Props) {
  const { user } = useAuth();
  const [original, setOriginal] = useState<Review | null>(null);
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [tags, setTags] = useState<ReviewTags>({});
  const [errors, setErrors] = useState<Errors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [loading, setLoading] = useState(props.mode === 'edit');
  const [submitting, setSubmitting] = useState(false);

  const reviewId = props.mode === 'edit' ? props.reviewId : null;

  // Edição: preenche o formulário com a avaliação atual
  useEffect(() => {
    if (!reviewId) return;
    reviewsApi
      .get(reviewId)
      .then((review) => {
        if (review.userId !== user?.id) {
          setFormError('Você só pode editar as suas próprias avaliações.');
          return;
        }
        setOriginal(review);
        setRating(review.rating);
        setComment(review.comment ?? '');
        setTags(review.tags ?? {});
      })
      .catch((e) => setFormError(e instanceof Error ? e.message : 'Não foi possível carregar a avaliação'))
      .finally(() => setLoading(false));
  }, [reviewId, user?.id]);

  function validate(): Errors {
    const next: Errors = {};
    if (rating < 1 || rating > 5) next.rating = 'Escolha uma nota de 1 a 5';
    if (Object.keys(tags).length === 0) next.tags = 'Avalie ao menos uma característica de acessibilidade';
    return next;
  }

  async function handleSubmit() {
    const next = validate();
    setErrors(next);
    setFormError(null);
    if (Object.keys(next).length) return;

    setSubmitting(true);
    try {
      if (props.mode === 'create') {
        await reviewsApi.create({
          placeId: props.placeId,
          rating,
          comment: comment.trim() || undefined,
          tags,
        });
        router.back();
      } else if (original) {
        const changes = diff(original, { rating, comment: comment.trim(), tags });
        if (Object.keys(changes).length) await reviewsApi.update(original.id, changes);
        router.back();
      }
    } catch (e) {
      if (props.mode === 'create' && e instanceof ApiError && e.status === 409 && user) {
        // Já existe avaliação deste usuário para o local: leva direto até ela
        const mine = await reviewsApi.findMine(props.placeId, user.id).catch(() => null);
        if (mine) {
          router.replace({ pathname: '/avaliacoes/[id]', params: { id: mine.id } });
          return;
        }
      }
      setFormError(e instanceof Error ? e.message : 'Não foi possível salvar a avaliação');
    } finally {
      setSubmitting(false);
    }
  }

  const title = props.mode === 'create' ? 'Avaliar local' : 'Editar avaliação';

  if (loading) {
    return (
      <View style={styles.center}>
        <Stack.Screen options={{ title }} />
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  const blocked = props.mode === 'edit' && !original;

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <Stack.Screen options={{ title }} />
      <ScrollView style={styles.container} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <ErrorMessage message={formError} />

        {!blocked && (
          <>
            <Text style={styles.sectionTitle} accessibilityRole="header">Nota geral</Text>
            <StarRatingInput value={rating} onChange={setRating} error={errors.rating} />

            <Text style={[styles.sectionTitle, styles.spaced]} accessibilityRole="header">
              Características de acessibilidade
            </Text>
            <Text style={styles.hint}>Marque só o que você observou. Toque de novo para desmarcar.</Text>
            <TagAssessmentPicker value={tags} onChange={setTags} error={errors.tags} />

            <View style={styles.spaced}>
              <TextField
                label="Comentário (opcional)"
                value={comment}
                onChangeText={setComment}
                multiline
                numberOfLines={4}
                textAlignVertical="top"
                style={styles.comment}
                placeholder="Conte como foi a experiência"
              />
            </View>

            <Button title={props.mode === 'create' ? 'Publicar avaliação' : 'Salvar alterações'} onPress={handleSubmit} loading={submitting} />
          </>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

/** Monta o PATCH só com o que mudou. `tags` vai inteiro, pois o backend substitui o mapa. */
function diff(original: Review, current: { rating: number; comment: string; tags: ReviewTags }): ReviewUpdateRequest {
  const changes: ReviewUpdateRequest = {};
  if (current.rating !== original.rating) changes.rating = current.rating;
  if (current.comment !== (original.comment ?? '')) changes.comment = current.comment;
  if (!sameTags(current.tags, original.tags ?? {})) changes.tags = current.tags;
  return changes;
}

function sameTags(a: ReviewTags, b: ReviewTags) {
  const keys = Object.keys(a) as (keyof ReviewTags)[];
  return keys.length === Object.keys(b).length && keys.every((k) => a[k] === b[k]);
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  container: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.md, paddingBottom: spacing.lg * 2 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
  sectionTitle: { fontSize: 17, fontWeight: '700', color: colors.text, marginBottom: spacing.sm },
  spaced: { marginTop: spacing.lg },
  hint: { fontSize: 14, color: colors.textMuted, marginBottom: spacing.md },
  comment: { minHeight: 100, paddingTop: spacing.sm },
});
