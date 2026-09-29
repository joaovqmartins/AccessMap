import { router } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, Text, View } from 'react-native';

import { reviewsApi } from '../api/reviews';
import type { Review } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import ReviewCard from '../components/ReviewCard';
import { colors, spacing } from '../constants/theme';

export default function ReviewListScreen({ placeId }: { placeId: string }) {
  const { user } = useAuth();
  const [reviews, setReviews] = useState<Review[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadPage = useCallback(
    async (pageNumber: number) => {
      setError(null);
      try {
        const result = await reviewsApi.list({ placeId, page: pageNumber });
        setReviews((prev) => (pageNumber === 0 ? result.content : [...prev, ...result.content]));
        setPage(result.page.number);
        setTotalPages(result.page.totalPages);
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Não foi possível carregar as avaliações');
      } finally {
        setLoading(false);
        setLoadingMore(false);
        setRefreshing(false);
      }
    },
    [placeId],
  );

  useEffect(() => {
    loadPage(0);
  }, [loadPage]);

  function loadMore() {
    if (loadingMore || loading || page + 1 >= totalPages) return;
    setLoadingMore(true);
    loadPage(page + 1);
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Carregando avaliações" />
      </View>
    );
  }

  return (
    <FlatList
      style={styles.container}
      contentContainerStyle={styles.content}
      data={reviews}
      keyExtractor={(item) => item.id}
      renderItem={({ item }) => (
        <ReviewCard
          review={item}
          isMine={item.userId === user?.id}
          onPress={() => router.push({ pathname: '/avaliacoes/[id]', params: { id: item.id } })}
        />
      )}
      onEndReached={loadMore}
      onEndReachedThreshold={0.4}
      refreshing={refreshing}
      onRefresh={() => {
        setRefreshing(true);
        loadPage(0);
      }}
      ListHeaderComponent={
        error ? (
          <View>
            <ErrorMessage message={error} />
            <Button title="Tentar novamente" variant="secondary" onPress={() => loadPage(reviews.length ? page + 1 : 0)} />
          </View>
        ) : null
      }
      ListEmptyComponent={!error ? <Text style={styles.empty}>Nenhuma avaliação ainda.</Text> : null}
      ListFooterComponent={loadingMore ? <ActivityIndicator style={styles.footer} color={colors.primary} /> : null}
    />
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.md },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
  empty: { textAlign: 'center', color: colors.textMuted, marginTop: spacing.lg, fontSize: 15 },
  footer: { marginVertical: spacing.md },
});
