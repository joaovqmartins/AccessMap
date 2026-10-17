import React, { useCallback, useRef, useState } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity, ScrollView, ActivityIndicator, FlatList, Linking, Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { router, useFocusEffect } from 'expo-router';

import { NEARBY_RADIUS_M } from '../api/googlePlaces';
import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import ErrorMessage from '../components/ErrorMessage';
import NearbyPlaceItem from '../components/NearbyPlaceItem';
import PlaceSearch from '../components/PlaceSearch';
import { ALL_NEARBY, PLACE_CATEGORIES, type PlaceCategory } from '../constants/categories';
import { MIN_TOUCH, colors, spacing } from '../constants/theme';
import { NO_FILTERS, useNearbyPlaces } from '../hooks/useNearbyPlaces';
import { useUserLocation, type LocationState } from '../hooks/useUserLocation';
import { formatDistance } from '../utils/distance';

function initials(name: string | undefined) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  return ((parts[0]?.[0] ?? '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}

function openPlace(placeId: string, name: string) {
  router.push({ pathname: '/locais/[placeId]', params: { placeId, name } });
}

export default function HomeScreen() {
  const { user } = useAuth();
  const { state: location, request: requestLocation } = useUserLocation();
  const [category, setCategory] = useState<PlaceCategory>(ALL_NEARBY);
  const nearby = useNearbyPlaces(location.status === 'ready' ? location.coords : null, category, NO_FILTERS);

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
          <TouchableOpacity style={styles.iconButton}>
            <Text style={styles.iconText}>☰</Text>
          </TouchableOpacity>
          
          {/* Imagem do Utilizador (Placeholder com iniciais) */}
          <TouchableOpacity
            style={styles.userAvatar}
            onPress={() => router.push('/perfil')}
            accessibilityRole="button"
            accessibilityLabel="Meu perfil"
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
                accessibilityState={{ selected }}
                accessibilityLabel={`Categoria ${c.label}`}
              >
                <Text style={[styles.optionText, selected && styles.optionTextSelected]}>{c.label}</Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </View>

      {/* 3. LOCAIS POR PERTO (o mapa da #26 entra aqui como outra visualização da mesma lista) */}
      <View style={styles.results}>
        {location.status !== 'ready' ? (
          <LocationNotice location={location} onRequest={requestLocation} />
        ) : (
          <FlatList
            data={nearby.places}
            keyExtractor={(p) => p.placeId}
            contentContainerStyle={styles.listContent}
            renderItem={({ item }) => (
              <NearbyPlaceItem place={item} onPress={() => openPlace(item.placeId, item.name)} />
            )}
            ListHeaderComponent={
              <>
                {nearby.places.length > 0 && (
                  <Text style={styles.listTitle} accessibilityRole="header" accessibilityLiveRegion="polite">
                    {category.label}: {nearby.places.length}{' '}
                    {nearby.places.length === 1 ? 'local' : 'locais'} até {formatDistance(NEARBY_RADIUS_M)}
                  </Text>
                )}
                <ErrorMessage message={nearby.summaryError} />
              </>
            }
            ListEmptyComponent={
              nearby.loading ? (
                <ActivityIndicator style={styles.spinner} size="large" color={colors.primary}
                  accessibilityLabel="Buscando locais por perto" />
              ) : nearby.error ? (
                <>
                  <ErrorMessage message={nearby.error} />
                  <Button title="Tentar novamente" variant="secondary" onPress={nearby.reload} />
                </>
              ) : (
                <Text style={styles.notice}>
                  Nenhum local desta categoria até {formatDistance(NEARBY_RADIUS_M)} de você.
                </Text>
              )
            }
            // Atribuição exigida pelo Google quando os dados dele aparecem sem o mapa dele
            ListFooterComponent={
              nearby.places.length > 0 ? <Text style={styles.attribution}>Google Maps</Text> : null
            }
          />
        )}
      </View>

      {/* 4. BARRA DE NAVEGAÇÃO INFERIOR */}
      <View style={styles.bottomNavigation}>
        <TouchableOpacity style={styles.navButton}>
          <Text style={[styles.navText, styles.navTextActive]}>Home</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.navButton}>
          <Text style={styles.navText}>Restaurantes</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.navButton}>
          <Text style={styles.navText}>Review</Text>
        </TouchableOpacity>
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
          <Text style={styles.notice}>Sem a localização não dá para mostrar locais por perto. {fallback}</Text>
          {location.canAskAgain ? (
            <Button title="Permitir localização" variant="secondary" onPress={onRequest} />
          ) : (
            Platform.OS !== 'web' && (
              <Button
                title="Abrir configurações"
                variant="secondary"
                onPress={() => Linking.openSettings()}
                accessibilityHint="Abre as configurações do aparelho para liberar a localização"
              />
            )
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
    backgroundColor: '#F5F5F5',
  },
  
  // --- Estilos do Cabeçalho ---
  header: {
    padding: 16,
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  topRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  iconButton: {
    padding: 8,
  },
  iconText: {
    fontSize: 24,
    color: '#333',
  },
  userAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#007BFF',
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    color: '#FFF',
    fontWeight: 'bold',
    fontSize: 16,
  },

  // --- Estilos das Opções (Filtros) ---
  optionsContainer: {
    paddingVertical: 12,
    backgroundColor: '#FFFFFF',
  },
  optionsContent: {
    paddingRight: 16,
  },
  optionPill: {
    minHeight: MIN_TOUCH,
    justifyContent: 'center',
    backgroundColor: '#E0E0E0',
    paddingHorizontal: 16,
    borderRadius: MIN_TOUCH / 2,
    marginLeft: 16,
  },
  optionPillSelected: {
    backgroundColor: colors.primary,
  },
  optionText: {
    color: '#333',
    fontWeight: '500',
  },
  optionTextSelected: {
    color: colors.onPrimary,
    fontWeight: '700',
  },

  // --- Estilos da Lista de Locais ---
  results: {
    flex: 1, // O flex: 1 faz esta área expandir e ocupar todo o espaço restante
  },
  listContent: {
    padding: spacing.md,
    flexGrow: 1,
  },
  listTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: colors.text,
    marginBottom: spacing.sm,
  },
  spinner: {
    marginTop: spacing.lg,
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
  attribution: {
    fontSize: 12,
    color: colors.textMuted,
    textAlign: 'right',
  },

  // --- Estilos da Navegação Inferior ---
  bottomNavigation: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    backgroundColor: '#FFFFFF',
    paddingVertical: 16,
    borderTopWidth: 1,
    borderTopColor: '#E0E0E0',
  },
  navButton: {
    alignItems: 'center',
  },
  navText: {
    color: '#888',
    fontSize: 14,
    fontWeight: '600',
  },
  navTextActive: {
    color: '#007BFF', // Destaca a rota atual
  },
});