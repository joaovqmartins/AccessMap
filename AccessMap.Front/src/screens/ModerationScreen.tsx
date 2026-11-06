import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, Text, View } from 'react-native';

import { adminApi } from '../api/admin';
import type { AdminReview } from '../api/types';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import ModerationCard, { type ModerationAction } from '../components/ModerationCard';
import { colors, spacing } from '../constants/theme';
import { confirm } from '../utils/dialog';

const CONFIRMATIONS: Record<ModerationAction, [title: string, message: string, label: string]> = {
  restore: ['Restaurar avaliação', 'Ela volta ao ar e à nota do local, e denúncias não a ocultam de novo.', 'Restaurar'],
  keep: ['Manter avaliação', 'Ela sai da fila e continua no ar.', 'Manter'],
  remove: ['Remover avaliação', 'Ela sai do ar, e a remoção conta contra o autor.', 'Remover'],
};

export default function ModerationScreen() {
  const [reviews, setReviews] = useState<AdminReview[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<{ id: string; action: ModerationAction } | null>(null);

  const loadPage = useCallback(async (pageNumber: number) => {
    try {
      const result = await adminApi.queue({ page: pageNumber });
      setReviews((prev) => (pageNumber === 0 ? result.content : [...prev, ...result.content]));
      setPage(result.page.number);
      setTotalPages(result.page.totalPages);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível carregar a fila');
    } finally {
      setLoading(false);
      setLoadingMore(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      loadPage(0);
    }, [loadPage]),
  );

  function loadMore() {
    if (loadingMore || loading || page + 1 >= totalPages) return;
    setLoadingMore(true);
    loadPage(page + 1);
  }

  async function handleAction(review: AdminReview, action: ModerationAction) {
    if (!(await confirm(...CONFIRMATIONS[action]))) return;

    setError(null);
    setBusy({ id: review.id, action });
    try {
      await adminApi[action](review.id);
    } catch (e) {
      // 404 ou 409: outro ADMIN já agiu. A recarga abaixo mostra a fila como ela está
      setError(e instanceof Error ? e.message : 'Não foi possível concluir a ação');
    }
    // Do início, e não só tirando o item: com a fila menor, a próxima página pularia avaliações
    await loadPage(0);
    setBusy(null);
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Carregando fila de moderação" />
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
        <ModerationCard
          review={item}
          busy={busy?.id === item.id ? busy.action : null}
          disabled={!!busy}
          onAction={(action) => handleAction(item, action)}
        />
      )}
      onEndReached={loadMore}
      onEndReachedThreshold={0.4}
      refreshing={refreshing}
      onRefresh={() => {
        setRefreshing(true);
        setError(null);
        loadPage(0);
      }}
      ListHeaderComponent={
        error ? (
          <View style={styles.errorBox}>
            <ErrorMessage message={error} />
            {!reviews.length && (
              <Button
                title="Tentar novamente"
                variant="secondary"
                onPress={() => {
                  setError(null);
                  loadPage(0);
                }}
              />
            )}
          </View>
        ) : null
      }
      ListEmptyComponent={!error ? <Text style={styles.empty}>Nenhuma avaliação aguardando moderação</Text> : null}
      ListFooterComponent={loadingMore ? <ActivityIndicator style={styles.footer} color={colors.primary} /> : null}
    />
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.md },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
  errorBox: { marginBottom: spacing.sm },
  empty: { textAlign: 'center', color: colors.textMuted, marginTop: spacing.lg, fontSize: 15 },
  footer: { marginVertical: spacing.md },
});
