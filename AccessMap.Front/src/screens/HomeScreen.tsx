import React, { useCallback, useRef, useState } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity, ScrollView, ActivityIndicator, Linking, Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { router, useFocusEffect } from 'expo-router';

import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import FiltersModal from '../components/FiltersModal';
import MainMenu from '../components/MainMenu';
import NearbyPlaceList from '../components/NearbyPlaceList';
import PlaceSearch from '../components/PlaceSearch';
import PlacesMap from '../components/PlacesMap';
import { ALL_NEARBY, PLACE_CATEGORIES, type PlaceCategory } from '../constants/categories';
import { TAG_LABELS } from '../constants/labels';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';
import { NO_FILTERS, hasFilters, useNearbyPlaces, type AccessibilityFilters } from '../hooks/useNearbyPlaces';
import { useUserLocation, type LocationState } from '../hooks/useUserLocation';

function initials(name: string | undefined) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  return ((parts[0]?.[0] ?? '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}

// O react-native-maps não roda no navegador: lá a Home mostra só a lista
const MAP_AVAILABLE = Platform.OS !== 'web';

// Atalho da barra inferior para a categoria mais procurada
const RESTAURANTS = PLACE_CATEGORIES.find((c) => c.key === 'restaurantes') ?? ALL_NEARBY;

function BottomTab({ label, selected, onPress, accessibilityLabel }: {
  label: string;
  selected: boolean;
  onPress: () => void;
  accessibilityLabel?: string;
}) {
  return (
    <TouchableOpacity
      style={styles.navButton}
      onPress={onPress}
      accessibilityRole="tab"
      // aria-selected além do accessibilityState: o react-native-web não repassa o `selected` para o DOM
      aria-selected={selected}
      accessibilityState={{ selected }}
      accessibilityLabel={accessibilityLabel ?? label}
    >
      <Text style={[styles.navText, selected && styles.navTextActive]}>{label}</Text>
    </TouchableOpacity>
  );
}

function openPlace(placeId: string, name: string) {
  router.push({ pathname: '/locais/[placeId]', params: { placeId, name } });
}

/** "Rampas e entradas, Elevadores e minhas necessidades"; `null` sem filtro. */
function describeFilters({ tags, useMyNeeds }: AccessibilityFilters) {
  const parts = tags.map((t) => TAG_LABELS[t]);
  if (useMyNeeds) parts.push('minhas necessidades');
  if (parts.length === 0) return null;
  return parts.length === 1 ? parts[0] : `${parts.slice(0, -1).join(', ')} e ${parts[parts.length - 1]}`;
}

export default function HomeScreen() {
  const { user } = useAuth();
  const { state: location, request: requestLocation } = useUserLocation();
  const [category, setCategory] = useState<PlaceCategory>(ALL_NEARBY);
  // "Minhas necessidades" começa desligado: com poucas avaliações, a lista viria quase vazia sem o usuário entender
  const [filters, setFilters] = useState<AccessibilityFilters>(NO_FILTERS);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const nearby = useNearbyPlaces(location.status === 'ready' ? location.coords : null, category, filters);
  const activeFilters = filters.tags.length + (filters.useMyNeeds ? 1 : 0);
  const filtersDescription = describeFilters(filters);
  // Lista é o padrão: funciona com leitor de tela e na web, onde não há mapa
  const [view, setView] = useState<'lista' | 'mapa'>('lista');

  // Ao voltar de um local (talvez recém-avaliado), atualiza as notas. Na primeira vez o hook já busca.
  const focusedOnce = useRef(false);
  const { reload } = nearby;
  useFocusEffect(
    useCallback(() => {
      if (focusedOnce.current) reload();
      focusedOnce.current = true;
    }, [reload]),
  );

  return (
    <SafeAreaView style={styles.container}>
      
      {/* 1. CABEÇALHO: Menu, Foto do Utilizador e Busca */}
      <View style={styles.header}>
        <View style={styles.topRow}>
          {/* Menu Hamburger */}
          <TouchableOpacity
            style={styles.iconButton}
            onPress={() => setMenuOpen(true)}
            accessibilityRole="button"
            accessibilityLabel="Abrir menu"
          >
            <Text style={styles.iconText}>☰</Text>
          </TouchableOpacity>
          
          {/* Imagem do Utilizador (Placeholder com iniciais) */}
          <TouchableOpacity
            style={styles.userAvatar}
            onPress={() => router.push('/perfil')}
            accessibilityRole="button"
            // As iniciais visíveis entram no nome (WCAG 2.5.3, rótulo no nome), depois do que o botão faz.
            // Sem parênteses: o leitor de tela e o axe tratam texto entre parênteses como comentário.
            accessibilityLabel={`Meu perfil, ${initials(user?.name)}`}
          >
            <Text style={styles.avatarText}>{initials(user?.name)}</Text>
          </TouchableOpacity>
        </View>

        {/* Barra de Busca */}
        <PlaceSearch onSelect={({ placeId, mainText }) => openPlace(placeId, mainText)} />
      </View>

      {/* 2. CATEGORIAS DE ESTABELECIMENTO */}
      <View style={styles.optionsContainer}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.optionsContent}>
          {PLACE_CATEGORIES.map((c) => {
            const selected = c.key === category.key;
            return (
              <TouchableOpacity
                key={c.key}
                style={[styles.optionPill, selected && styles.optionPillSelected]}
                onPress={() => setCategory(c)}
                accessibilityRole="button"
                aria-pressed={selected}
                accessibilityState={{ selected }}
                accessibilityLabel={`Categoria ${c.label}`}
              >
                <Text style={[styles.optionText, selected && styles.optionTextSelected]}>{c.label}</Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
        <View style={styles.toolbar}>
          {MAP_AVAILABLE && (
            <View style={styles.segmented} accessibilityRole="radiogroup" accessibilityLabel="Visualização">
              {(['lista', 'mapa'] as const).map((v) => {
                const selected = view === v;
                return (
                  <TouchableOpacity
                    key={v}
                    style={[styles.segment, selected && styles.optionPillSelected]}
                    onPress={() => setView(v)}
                    accessibilityRole="radio"
                    aria-checked={selected}
                    accessibilityState={{ checked: selected }}
                    accessibilityLabel={v === 'lista' ? 'Ver em lista' : 'Ver no mapa'}
                  >
                    <Text style={[styles.optionText, selected && styles.optionTextSelected]}>
                      {v === 'lista' ? 'Lista' : 'Mapa'}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>
          )}
          <View style={styles.toolbarSpacer} />
          <TouchableOpacity
            style={[styles.toolButton, hasFilters(filters) && styles.optionPillSelected]}
            onPress={() => setFiltersOpen(true)}
            accessibilityRole="button"
            accessibilityLabel={activeFilters ? `Filtros de acessibilidade, ${activeFilters} ativos` : 'Filtros de acessibilidade'}
          >
            <Text style={[styles.optionText, hasFilters(filters) && styles.optionTextSelected]}>
              Filtros{activeFilters ? ` (${activeFilters})` : ''}
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* 3. LOCAIS POR PERTO */}
      <View style={styles.results}>
        {location.status !== 'ready' ? (
          <LocationNotice location={location} onRequest={requestLocation} />
        ) : view === 'mapa' && !nearby.loading && !nearby.error ? (
          <>
            {(filtersDescription || nearby.summaryError) && (
              <Text style={styles.mapBanner} accessibilityLiveRegion="polite">
                {nearby.summaryError ?? `Filtrando por: ${filtersDescription}`}
              </Text>
            )}
            <PlacesMap
              center={location.coords}
              places={nearby.places}
              unrated={nearby.unrated}
              onOpen={(p) => openPlace(p.placeId, p.name)}
            />
          </>
        ) : (
          <NearbyPlaceList
            places={nearby.places}
            unrated={nearby.unrated}
            total={nearby.total}
            loading={nearby.loading}
            error={nearby.error}
            summaryError={nearby.summaryError}
            categoryLabel={category.label}
            filtersDescription={filtersDescription}
            onRetry={nearby.reload}
            onClearFilters={() => setFilters(NO_FILTERS)}
            onOpen={(p) => openPlace(p.placeId, p.name)}
          />
        )}
      </View>

      {filtersOpen && (
        <FiltersModal
          value={filters}
          needs={user?.accessibilityNeeds ?? []}
          onClose={() => setFiltersOpen(false)}
          onApply={(f) => {
            setFilters(f);
            setFiltersOpen(false);
          }}
        />
      )}

      {menuOpen && <MainMenu onClose={() => setMenuOpen(false)} />}

      {/* 4. BARRA DE NAVEGAÇÃO INFERIOR */}
      <View style={styles.bottomNavigation} accessibilityRole="tablist">
        <BottomTab
          label="Home"
          selected={category.key === ALL_NEARBY.key}
          onPress={() => {
            setCategory(ALL_NEARBY);
            setFilters(NO_FILTERS);
          }}
        />
        <BottomTab
          label="Restaurantes"
          selected={category.key === RESTAURANTS.key}
          onPress={() => setCategory(RESTAURANTS)}
        />
        <BottomTab
          label="Review"
          // O nome começa pelo texto visível (WCAG 2.5.3): quem usa controle por voz fala "Review"
          accessibilityLabel="Review: minhas avaliações"
          selected={false}
          onPress={() => router.push('/minhas-avaliacoes')}
        />
      </View>

    </SafeAreaView>
  );
}

/** O que aparece no lugar da lista enquanto não há localização. A busca por nome continua funcionando. */
function LocationNotice({ location, onRequest }: { location: LocationState; onRequest: () => void }) {
  const fallback = 'Você ainda pode buscar um local pelo nome, no campo acima.';

  switch (location.status) {
    case 'checking':
    case 'locating':
    case 'ready':
      return (
        <View style={styles.noticeBox}>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={styles.notice} accessibilityLiveRegion="polite">Obtendo sua localização...</Text>
        </View>
      );
    case 'undetermined':
      return (
        <View style={styles.noticeBox}>
          <Text style={styles.notice}>
            Permita o acesso à localização para ver locais por perto e como é a acessibilidade de cada um.
          </Text>
          <Button title="Permitir localização" onPress={onRequest} />
        </View>
      );
    case 'denied':
      return (
        <View style={styles.noticeBox}>
          <Text style={styles.notice} accessibilityLiveRegion="polite">
            Sem a localização não dá para mostrar locais por perto. {fallback}
          </Text>
          {Platform.OS === 'web' && (
            // No navegador, depois de negar, o pedido não aparece de novo: o botão sozinho não fazia nada visível
            <Text style={styles.notice}>
              Para liberar, clique no cadeado ao lado do endereço do site, permita a localização e tente de novo.
            </Text>
          )}
          {Platform.OS === 'web' ? (
            <Button title="Tentar de novo" variant="secondary" onPress={onRequest} />
          ) : location.canAskAgain ? (
            <Button title="Permitir localização" variant="secondary" onPress={onRequest} />
          ) : (
            <Button
              title="Abrir configurações"
              variant="secondary"
              onPress={() => Linking.openSettings()}
              accessibilityHint="Abre as configurações do aparelho para liberar a localização"
            />
          )}
        </View>
      );
    case 'unavailable':
      return (
        <View style={styles.noticeBox}>
          <Text style={styles.notice} accessibilityRole="alert">{location.message} {fallback}</Text>
          <Button title="Tentar novamente" variant="secondary" onPress={onRequest} />
        </View>
      );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  
  // --- Estilos do Cabeçalho ---
  header: {
    padding: 16,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  topRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  iconButton: {
    minWidth: MIN_TOUCH,
    minHeight: MIN_TOUCH,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconText: {
    fontSize: 24,
    color: colors.text,
  },
  userAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.primary,
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    color: colors.onPrimary,
    fontWeight: 'bold',
    fontSize: 16,
  },

  // --- Estilos das Opções (Filtros) ---
  optionsContainer: {
    paddingVertical: 12,
    backgroundColor: colors.surface,
  },
  optionsContent: {
    paddingRight: 16,
  },
  optionPill: {
    minHeight: MIN_TOUCH,
    justifyContent: 'center',
    backgroundColor: colors.border,
    paddingHorizontal: 16,
    borderRadius: MIN_TOUCH / 2,
    marginLeft: 16,
  },
  optionPillSelected: {
    backgroundColor: colors.primary,
  },
  optionText: {
    color: colors.text,
    fontWeight: '500',
  },
  optionTextSelected: {
    color: colors.onPrimary,
    fontWeight: '700',
  },

  toolbar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    gap: spacing.sm,
    paddingHorizontal: 16,
    paddingTop: spacing.sm,
  },
  toolbarSpacer: {
    flex: 1,
  },
  segmented: {
    flexDirection: 'row',
    backgroundColor: colors.border,
    borderRadius: 8,
    overflow: 'hidden',
  },
  segment: {
    minHeight: MIN_TOUCH,
    minWidth: 72,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 16,
  },
  mapBanner: {
    fontSize: 14,
    color: colors.text,
    backgroundColor: colors.surface,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  toolButton: {
    minHeight: MIN_TOUCH,
    justifyContent: 'center',
    backgroundColor: colors.border,
    paddingHorizontal: 16,
    borderRadius: 8,
  },

  // --- Estilos dos Resultados ---
  results: {
    flex: 1, // O flex: 1 faz esta área expandir e ocupar todo o espaço restante
  },
  noticeBox: {
    flex: 1,
    justifyContent: 'center',
    padding: spacing.lg,
    gap: spacing.md,
  },
  notice: {
    fontSize: 15,
    color: colors.textMuted,
    textAlign: 'center',
  },

  // --- Estilos da Navegação Inferior ---
  bottomNavigation: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    backgroundColor: colors.surface,
    paddingVertical: spacing.xs,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  navButton: {
    flex: 1,
    minHeight: MIN_TOUCH + spacing.sm,
    alignItems: 'center',
    justifyContent: 'center',
  },
  navText: {
    // #888 tinha contraste 3,5:1 com o branco; o textMuted passa dos 4,5:1 do WCAG AA
    color: colors.textMuted,
    fontSize: 14,
    fontWeight: '600',
  },
  navTextActive: {
    color: colors.primary, // Destaca a rota atual
  },
});