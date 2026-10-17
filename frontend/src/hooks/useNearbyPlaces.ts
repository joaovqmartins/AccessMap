import { useCallback, useEffect, useRef, useState } from 'react';

import { searchNearby, type NearbyPlace } from '../api/googlePlaces';
import { placesApi } from '../api/places';
import type { AccessibilityTag, PlaceSummary } from '../api/types';
import type { PlaceCategory } from '../constants/categories';
import { distanceInMeters } from '../utils/distance';
import type { Coords } from './useUserLocation';

export interface NearbyResult extends NearbyPlace {
  distanceM: number;
  /** Agregados do AccessMap; `null` só quando o backend não respondeu. */
  summary: PlaceSummary | null;
}

export interface AccessibilityFilters {
  /** O local precisa ter todas: maioria estrita dos votos em ADEQUADO (regra do backend). */
  tags: AccessibilityTag[];
  /** Soma as tags essenciais para as necessidades do perfil; o mapeamento fica no backend (#73). */
  useMyNeeds: boolean;
}

export const NO_FILTERS: AccessibilityFilters = { tags: [], useMyNeeds: false };

export function hasFilters(filters: AccessibilityFilters) {
  return filters.tags.length > 0 || filters.useMyNeeds;
}

type Result = {
  key: string;
  places: NearbyResult[];
  /** Com filtro: locais que ninguém avaliou ainda. O backend os tira do filtro, mas não dá para dizer que não atendem. */
  unrated: NearbyResult[];
  /** Quantos locais o Google trouxe, antes do filtro. */
  total: number;
  error: string | null;
  /** O Google respondeu mas o backend não: a lista aparece sem as notas e sem filtro. */
  summaryError: string | null;
};

/**
 * Locais da categoria em volta de `coords`: o Google diz quais são e a distância (ordem), o backend
 * diz como é a acessibilidade de cada um e quais atendem aos filtros. A lista e o mapa usam o mesmo resultado.
 */
export function useNearbyPlaces(coords: Coords | null, category: PlaceCategory, filters: AccessibilityFilters) {
  const [result, setResult] = useState<Result | null>(null);
  const [nonce, setNonce] = useState(0);
  // Os locais do Google não mudam durante o uso e cada chamada é cobrada: trocar de categoria ou de
  // filtro e voltar não repete a busca. As notas vêm sempre do backend, que é nosso e muda a cada avaliação.
  const googleCache = useRef(new Map<string, NearbyPlace[]>());

  const googleKey = coords ? `${coords.latitude},${coords.longitude}|${category.key}` : null;
  const filtered = hasFilters(filters);
  const filterKey = `${[...filters.tags].sort().join(',')}|${filters.useMyNeeds}`;
  const key = googleKey && `${googleKey}|${filterKey}`;

  useEffect(() => {
    if (!coords || !googleKey || !key) return;

    let cancelled = false;
    const controller = new AbortController();

    (async () => {
      let places = googleCache.current.get(googleKey);
      try {
        if (!places) {
          places = await searchNearby(coords, category.types, controller.signal);
          googleCache.current.set(googleKey, places);
        }
      } catch (e) {
        if (cancelled) return;
        const error = e instanceof Error ? e.message : 'Erro na busca';
        setResult({ key, places: [], unrated: [], total: 0, error, summaryError: null });
        return;
      }

      const placeIds = places.map((p) => p.placeId);
      let summaries: PlaceSummary[] = [];
      let matching: Set<string> | null = null;
      let summaryError: string | null = null;
      if (places.length > 0) {
        try {
          // Com filtro, duas chamadas ao nosso backend: os agregados de todos (para separar quem
          // ninguém avaliou) e os que atendem. Nenhuma chamada a mais ao Google.
          const [all, matched] = await Promise.all([
            placesApi.batch({ placeIds }),
            filtered ? placesApi.batch({ placeIds, tags: filters.tags, useMyNeeds: filters.useMyNeeds }) : null,
          ]);
          summaries = all;
          matching = matched && new Set(matched.map((s) => s.placeId));
        } catch {
          summaryError = filtered
            ? 'Não foi possível carregar as avaliações: os filtros não foram aplicados.'
            : 'Não foi possível carregar as avaliações de acessibilidade.';
        }
      }
      if (cancelled) return;

      const byId = new Map(summaries.map((s) => [s.placeId, s]));
      const results = places.map((p) => ({
        ...p,
        distanceM: distanceInMeters(coords, p),
        summary: byId.get(p.placeId) ?? null,
      }));
      setResult({
        key,
        places: matching ? results.filter((p) => matching.has(p.placeId)) : results,
        unrated: matching
          ? results.filter((p) => !matching.has(p.placeId) && p.summary?.reviewCount === 0)
          : [],
        total: results.length,
        error: null,
        summaryError,
      });
    })();

    // Troca de categoria ou filtro no meio da busca: descarta a anterior para não mostrar resultado velho
    return () => {
      cancelled = true;
      controller.abort();
    };
    // coords, category e filters entram pela key
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, nonce]);

  /** Busca as notas de novo (ex.: ao voltar de uma avaliação), mantendo a lista na tela. */
  const reload = useCallback(() => setNonce((n) => n + 1), []);

  const current = key && result?.key === key ? result : null;
  return {
    places: current?.places ?? [],
    unrated: current?.unrated ?? [],
    total: current?.total ?? 0,
    error: current?.error ?? null,
    summaryError: current?.summaryError ?? null,
    loading: !!key && !current,
    reload,
  };
}
