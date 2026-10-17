import { ActivityIndicator, SectionList, StyleSheet, Text, View } from 'react-native';

import { NEARBY_RADIUS_M } from '../api/googlePlaces';
import { colors, spacing } from '../constants/theme';
import type { NearbyResult } from '../hooks/useNearbyPlaces';
import { formatDistance } from '../utils/distance';
import Button from './Button';
import ErrorMessage from './ErrorMessage';
import NearbyPlaceItem from './NearbyPlaceItem';

type Props = {
  places: NearbyResult[];
  /** Só com filtro: locais que ninguém avaliou, mostrados à parte. */
  unrated: NearbyResult[];
  loading: boolean;
  error: string | null;
  summaryError: string | null;
  categoryLabel: string;
  /** Ex.: "Rampas e entradas e minhas necessidades"; `null` sem filtro. */
  filtersDescription: string | null;
  onRetry: () => void;
  onClearFilters: () => void;
  onOpen: (place: NearbyResult) => void;
};

type Section = { key: 'matching' | 'unrated'; title?: string; hint?: string; data: NearbyResult[] };

const RADIUS = formatDistance(NEARBY_RADIUS_M);

export default function NearbyPlaceList({
  places, unrated, loading, error, summaryError, categoryLabel, filtersDescription, onRetry, onClearFilters, onOpen,
}: Props) {
  if (loading) {
    return (
      <ActivityIndicator style={styles.spinner} size="large" color={colors.primary}
        accessibilityLabel="Buscando locais por perto" />
    );
  }
  if (error) {
    return (
      <View style={styles.content}>
        <ErrorMessage message={error} />
        <Button title="Tentar novamente" variant="secondary" onPress={onRetry} />
      </View>
    );
  }

  const total = places.length + unrated.length;
  const sections: Section[] = [{ key: 'matching', data: places }];
  if (unrated.length > 0) {
    sections.push({
      key: 'unrated',
      title: 'Ainda sem avaliação',
      hint: 'Ninguém avaliou estes locais, então não dá para saber se atendem aos filtros. Que tal ser a primeira pessoa?',
      data: unrated,
    });
  }

  return (
    <SectionList
      sections={sections}
      keyExtractor={(p) => p.placeId}
      contentContainerStyle={styles.content}
      stickySectionHeadersEnabled={false}
      renderItem={({ item }) => <NearbyPlaceItem place={item} onPress={() => onOpen(item)} />}
      renderSectionHeader={({ section }) =>
        section.title ? (
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle} accessibilityRole="header">
              {section.title} ({section.data.length})
            </Text>
            {!!section.hint && <Text style={styles.hint}>{section.hint}</Text>}
          </View>
        ) : null
      }
      renderSectionFooter={({ section }) =>
        section.key === 'matching' && section.data.length === 0 && total > 0 ? (
          <Text style={styles.notice}>Nenhum local avaliado atende a estes filtros até {RADIUS} de você.</Text>
        ) : null
      }
      ListHeaderComponent={
        <>
          {total > 0 && (
            <Text style={styles.title} accessibilityRole="header" accessibilityLiveRegion="polite">
              {filtersDescription
                ? `${categoryLabel}: ${places.length} de ${total} ${total === 1 ? 'local atende' : 'locais atendem'}`
                : `${categoryLabel}: ${total} ${total === 1 ? 'local' : 'locais'} até ${RADIUS}`}
            </Text>
          )}
          {filtersDescription && (
            <View style={styles.filtersRow}>
              <Text style={styles.filtersText}>Filtrando por: {filtersDescription}</Text>
              <Button title="Limpar filtros" variant="secondary" onPress={onClearFilters} />
            </View>
          )}
          <ErrorMessage message={summaryError} />
        </>
      }
      ListEmptyComponent={
        <Text style={styles.notice}>Nenhum local desta categoria até {RADIUS} de você.</Text>
      }
      // Atribuição exigida pelo Google quando os dados dele aparecem sem o mapa dele
      ListFooterComponent={total > 0 ? <Text style={styles.attribution}>Google Maps</Text> : null}
    />
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.md, flexGrow: 1 },
  spinner: { marginTop: spacing.lg },
  title: { fontSize: 15, fontWeight: '700', color: colors.text, marginBottom: spacing.sm },
  filtersRow: { gap: spacing.sm, marginBottom: spacing.md },
  filtersText: { fontSize: 14, color: colors.text },
  sectionHeader: { marginTop: spacing.md, marginBottom: spacing.sm },
  sectionTitle: { fontSize: 15, fontWeight: '700', color: colors.text },
  hint: { fontSize: 14, color: colors.textMuted, marginTop: 2 },
  notice: { fontSize: 15, color: colors.textMuted, textAlign: 'center', marginVertical: spacing.md },
  attribution: { fontSize: 12, color: colors.textMuted, textAlign: 'right' },
});
