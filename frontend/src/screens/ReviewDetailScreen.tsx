import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Alert, ScrollView, StyleSheet, View } from 'react-native';

import { ApiError } from '../api/client';
import { reviewsApi } from '../api/reviews';
import type { Review } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import ReviewCard from '../components/ReviewCard';
import { colors, spacing } from '../constants/theme';

export default function ReviewDetailScreen({ reviewId }: { reviewId: string }) {
  const { user } = useAuth();
  const [review, setReview] = useState<Review | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Recarrega ao voltar da edição
  useFocusEffect(
    useCallback(() => {
      reviewsApi
        .get(reviewId)
        .then((data) => {
          setReview(data);
          setError(null);
        })
        .catch((e) =>
          setError(
            e instanceof ApiError && e.status === 404
              ? 'Esta avaliação não existe mais.'
              : e instanceof Error ? e.message : 'Não foi possível carregar a avaliação',
          ),
        )
        .finally(() => setLoading(false));
    }, [reviewId]),
  );

  const isMine = !!review && review.userId === user?.id;

  function confirmDelete() {
    Alert.alert('Excluir avaliação', 'Tem certeza? Esta ação não pode ser desfeita.', [
      { text: 'Cancelar', style: 'cancel' },
      { text: 'Excluir', style: 'destructive', onPress: handleDelete },
    ]);
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      await reviewsApi.remove(reviewId);
      router.back();
    } catch (e) {
      setError(
        e instanceof ApiError && e.status === 403
          ? 'Você só pode excluir as suas próprias avaliações.'
          : e instanceof Error ? e.message : 'Não foi possível excluir a avaliação',
      );
      setDeleting(false);
    }
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Carregando avaliação" />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <ErrorMessage message={error} />
      {review && <ReviewCard review={review} isMine={isMine} />}
      {isMine && (
        <View style={styles.actions}>
          <Button
            title="Editar avaliação"
            onPress={() => router.push({ pathname: '/avaliacoes/[id]/editar', params: { id: reviewId } })}
          />
          <Button title="Excluir avaliação" variant="danger" onPress={confirmDelete} loading={deleting} />
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.md },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
  actions: { gap: spacing.sm, marginTop: spacing.md },
});
