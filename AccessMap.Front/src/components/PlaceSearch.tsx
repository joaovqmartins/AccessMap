import { useState } from 'react';
import { ActivityIndicator, Keyboard, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { isGooglePlacesConfigured, type PlaceSuggestion } from '../api/googlePlaces';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';
import { MIN_QUERY_LENGTH, usePlaceAutocomplete } from '../hooks/usePlaceAutocomplete';
import ErrorMessage from './ErrorMessage';

type Props = {
  onSelect: (place: PlaceSuggestion) => void;
};

export default function PlaceSearch({ onSelect }: Props) {
  const [query, setQuery] = useState('');
  const { suggestions, loading, error, endSession } = usePlaceAutocomplete(query);

  if (!isGooglePlacesConfigured()) {
    return (
      <Text style={styles.notice} accessibilityRole="alert">
        Busca indisponível: configure a chave do Google em AccessMap.Front/.env.local (veja o README).
      </Text>
    );
  }

  const searched = query.trim().length >= MIN_QUERY_LENGTH;

  function select(place: PlaceSuggestion) {
    Keyboard.dismiss();
    setQuery('');
    endSession();
    onSelect(place);
  }

  return (
    <View>
      <View style={styles.inputRow}>
        <TextInput
          style={styles.input}
          value={query}
          onChangeText={setQuery}
          placeholder="Buscar locais..."
          placeholderTextColor={colors.textMuted}
          accessibilityLabel="Buscar local"
          accessibilityHint={`Digite pelo menos ${MIN_QUERY_LENGTH} letras para ver sugestões`}
          autoCorrect={false}
          returnKeyType="search"
        />
        {loading && <ActivityIndicator style={styles.trailing} color={colors.primary} />}
        {!loading && query.length > 0 && (
          <Pressable
            style={styles.trailing}
            onPress={() => setQuery('')}
            accessibilityRole="button"
            accessibilityLabel="Limpar busca"
            hitSlop={8}
          >
            <Text style={styles.clearText}>✕</Text>
          </Pressable>
        )}
      </View>

      {searched && !loading && (
        <View style={styles.results}>
          <ErrorMessage message={error} />
          {!error && (
            <Text style={styles.status} accessibilityLiveRegion="polite">
              {suggestions.length === 0
                ? 'Nenhum local encontrado'
                : `${suggestions.length} ${suggestions.length === 1 ? 'local encontrado' : 'locais encontrados'}`}
            </Text>
          )}
          {suggestions.map((s) => (
            <Pressable
              key={s.placeId}
              onPress={() => select(s)}
              accessibilityRole="button"
              accessibilityLabel={s.secondaryText ? `${s.mainText}, ${s.secondaryText}` : s.mainText}
              accessibilityHint="Abre os detalhes de acessibilidade do local"
              style={({ pressed }) => [styles.suggestion, pressed && styles.pressed]}
            >
              <Text style={styles.mainText} numberOfLines={1}>{s.mainText}</Text>
              {!!s.secondaryText && (
                <Text style={styles.secondaryText} numberOfLines={1}>{s.secondaryText}</Text>
              )}
            </Pressable>
          ))}
          {/* Atribuição exigida pelo Google quando as sugestões aparecem sem o mapa dele */}
          {suggestions.length > 0 && <Text style={styles.attribution}>Google Maps</Text>}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  inputRow: { flexDirection: 'row', alignItems: 'center' },
  input: {
    flex: 1,
    minHeight: MIN_TOUCH,
    backgroundColor: '#F0F0F0',
    borderRadius: 8,
    paddingLeft: spacing.md,
    paddingRight: MIN_TOUCH,
    fontSize: 16,
    color: colors.text,
  },
  trailing: {
    position: 'absolute',
    right: 0,
    width: MIN_TOUCH,
    height: MIN_TOUCH,
    alignItems: 'center',
    justifyContent: 'center',
  },
  clearText: { fontSize: 18, color: colors.textMuted },
  results: {
    marginTop: spacing.sm,
    backgroundColor: colors.surface,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.border,
    paddingTop: spacing.sm,
  },
  status: { fontSize: 13, color: colors.textMuted, paddingHorizontal: spacing.md, paddingBottom: spacing.xs },
  suggestion: {
    minHeight: MIN_TOUCH,
    justifyContent: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  pressed: { backgroundColor: colors.background },
  mainText: { fontSize: 16, fontWeight: '600', color: colors.text },
  secondaryText: { fontSize: 14, color: colors.textMuted, marginTop: 2 },
  attribution: {
    fontSize: 12,
    color: colors.textMuted,
    textAlign: 'right',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  notice: {
    fontSize: 14,
    color: colors.warning,
    backgroundColor: '#FFF4E5',
    borderRadius: 8,
    padding: spacing.sm + 4,
  },
});
