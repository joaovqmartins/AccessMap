import { Link, Stack, router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';

import { placesApi } from '../api/places';
import { reviewsApi } from '../api/reviews';
import type { AccessibilityTag, Place, Review } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import ReviewCard from '../components/ReviewCard';
import StarRating from '../components/StarRating';
import TagStatsBar from '../components/TagStatsBar';
import { ALL_TAGS } from '../constants/labels';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';

const PREVIEW_SIZE = 3;

type Props = {
  placeId: string;
  /** Nome vindo da busca (#23). O backend guarda só o Place ID. */
  name?: string;
};

export default function PlaceDetailsScreen({ placeId, name }: Props) {
  const { user } = useAuth();
  const [place, setPlace] = useState<Place | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [myReview, setMyReview] = useState<Review | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [placeData, page, mine] = await Promise.all([
        placesApi.get(placeId),
        reviewsApi.list({ placeId, size: PREVIEW_SIZE }),
        // O backend aceita uma avaliação por usuário e local: se já existir, o CTA leva até ela
        user ? reviewsApi.findMine(placeId, user.id) : Promise.resolve(null),
      ]);
      setPlace(placeData);
      setReviews(page.content);
      setMyReview(mine);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível carregar o local');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [placeId, user]);

  // Recarrega ao voltar para a tela (ex.: depois de criar ou editar uma avaliação)
  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const title = name || 'Detalhes do local';
  const tagsWithStats = ALL_TAGS.filter((tag) => place?.tagStats?.[tag]) as AccessibilityTag[];

  if (loading) {
    return (
      <View style={styles.center}>
        <Stack.Screen options={{ title }} />
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Carregando local" />
      </View>
    );
  }

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={
        <RefreshControl
          refreshing={refreshing}
          onRefresh={() => {
            setRefreshing(true);
            load();
          }}
        />
      }
    >
      <Stack.Screen options={{ title }} />

      {error ? (
        <>
          <ErrorMessage message={error} />
          <Button title="Tentar novamente" variant="secondary" onPress={() => { setLoading(true); load(); }} />
        </>
      ) : (
        <>
          <View style={styles.section}>
            <Text style={styles.placeName} accessibilityRole="header">{name || placeId}</Text>
            {place && place.reviewCount > 0 ? (
              <View style={styles.scoreRow}>
                <Text style={styles.score}>{place.averageScore.toFixed(1).replace('.', ',')}</Text>
                <View>
                  <StarRating value={place.averageScore} size={22} />
                  <Text style={styles.muted}>
                    {place.reviewCount} {place.reviewCount === 1 ? 'avaliação' : 'avaliações'}
                  </Text>
                </View>
              </View>
            ) : (
              <Text style={styles.muted}>
                Este local ainda não tem avaliações de acessibilidade. Seja a primeira pessoa a avaliar!
              </Text>
            )}
          </View>

          <View style={styles.cta}>
            {!user ? (
              <Button title="Entrar para avaliar" onPress={() => router.push('/login')} />
            ) : myReview ? (
              <Button
                title="Ver minha avaliação"
                variant="secondary"
                onPress={() => router.push({ pathname: '/avaliacoes/[id]', params: { id: myReview.id } })}
              />
            ) : (
              <Button
                title="Avaliar este local"
                onPress={() => router.push({ pathname: '/locais/[placeId]/avaliar', params: { placeId } })}
              />
            )}
          </View>

          {tagsWithStats.length > 0 && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle} accessibilityRole="header">Acessibilidade por característica</Text>
              {tagsWithStats.map((tag) => (
                <TagStatsBar key={tag} tag={tag} stats={place!.tagStats[tag]!} />
              ))}
            </View>
          )}

          {reviews.length > 0 && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle} accessibilityRole="header">Avaliações recentes</Text>
              {reviews.map((review) => (
                <ReviewCard
                  key={review.id}
                  review={review}
                  isMine={review.userId === user?.id}
                  onPress={() => router.push({ pathname: '/avaliacoes/[id]', params: { id: review.id } })}
                />
              ))}
              {(place?.reviewCount ?? 0) > reviews.length && (
                <Link
                  href={{ pathname: '/locais/[placeId]/avaliacoes', params: { placeId } }}
                  style={styles.link}
                  accessibilityRole="link"
                >
                  Ver todas as {place!.reviewCount} avaliações
                </Link>
              )}
            </View>
          )}
        </>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.md },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
  section: {
    backgroundColor: colors.surface,
    borderRadius: 8,
    padding: spacing.md,
    marginBottom: spacing.md,
  },
  placeName: { fontSize: 22, fontWeight: 'bold', color: colors.text, marginBottom: spacing.sm },
  scoreRow: { flexDirection: 'row', alignItems: 'center' },
  score: { fontSize: 40, fontWeight: 'bold', color: colors.text, marginRight: spacing.md },
  sectionTitle: { fontSize: 17, fontWeight: '700', color: colors.text, marginBottom: spacing.md },
  muted: { fontSize: 14, color: colors.textMuted },
  cta: { marginBottom: spacing.md },
  link: {
    minHeight: MIN_TOUCH,
    textAlign: 'center',
    textAlignVertical: 'center',
    color: colors.primary,
    fontSize: 15,
    fontWeight: '600',
  },
});
