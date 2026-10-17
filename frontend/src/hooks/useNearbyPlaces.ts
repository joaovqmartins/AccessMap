import { useCallback, useEffect, useRef, useState } from 'react';

import { searchNearby, type NearbyPlace } from '../api/googlePlaces';
import { placesApi } from '../api/places';
import type { PlaceSummary } from '../api/types';
import type { PlaceCategory } from '../constants/categories';
import { distanceInMeters } from '../utils/distance';
import type { Coords } from './useUserLocation';

export interface NearbyResult extends NearbyPlace {
  distanceM: number;
  /** Agregados do AccessMap; `null` só quando o backend não respondeu. */
  summary: PlaceSummary | null;
}

type Result = {
  key: string;
  places: NearbyResult[];
  error: string | null;
  /** O Google respondeu mas o backend não: a lista aparece sem as notas. */
  summaryError: string | null;
};

/**
 * Locais da categoria em volta de `coords`: o Google diz quais são e a distância (ordem), o backend
 * diz como é a acessibilidade de cada um. Fica fora da tela para o mapa (#26) reaproveitar.
 */
export function useNearbyPlaces(coords: Coords | null, category: PlaceCategory) {
  const [result, setResult] = useState<Result | null>(null);
  const [nonce, setNonce] = useState(0);
  // Os locais do Google não mudam durante o uso e cada chamada é cobrada: trocar de categoria e
  // voltar não repete a busca. As notas vêm sempre do backend, que é nosso e muda a cada avaliação.
  const googleCache = useRef(new Map<string, NearbyPlace[]>());

  const key = coords ? `${coords.latitude},${coords.longitude}|${category.key}` : null;

  useEffect(() => {
    if (!coords || !key) return;

    let cancelled = false;
    const controller = new AbortController();

    (async () => {
      let places = googleCache.current.get(key);
      try {
        if (!places) {
          places = await searchNearby(coords, category.types, controller.signal);
          googleCache.current.set(key, places);
        }
      } catch (e) {
        if (cancelled) return;
        setResult({ key, places: [], error: e instanceof Error ? e.message : 'Erro na busca', summaryError: null });
        return;
      }

      let summaries: PlaceSummary[] = [];
      let summaryError: string | null = null;
      if (places.length > 0) {
        try {
          summaries = await placesApi.batch({ placeIds: places.map((p) => p.placeId) });
        } catch {
          summaryError = 'Não foi possível carregar as avaliações de acessibilidade.';
        }
      }
      if (cancelled) return;

      const byId = new Map(summaries.map((s) => [s.placeId, s]));
      setResult({
        key,
        places: places.map((p) => ({
          ...p,
          distanceM: distanceInMeters(coords, p),
          summary: byId.get(p.placeId) ?? null,
        })),
        error: null,
        summaryError,
      });
    })();

    // Troca de categoria no meio da busca: descarta a anterior para não mostrar resultado velho
    return () => {
      cancelled = true;
      controller.abort();
    };
    // coords e category entram pela key
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, nonce]);

  /** Busca as notas de novo (ex.: ao voltar de uma avaliação), mantendo a lista na tela. */
  const reload = useCallback(() => setNonce((n) => n + 1), []);

  const current = key && result?.key === key ? result : null;
  return {
    places: current?.places ?? [],
    error: current?.error ?? null,
    summaryError: current?.summaryError ?? null,
    loading: !!key && !current,
    reload,
  };
}
