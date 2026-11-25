import { useEffect, useState } from 'react';

import { getPlaceLocation, isGooglePlacesConfigured, type PlaceLocation } from '../api/googlePlaces';

type Result = { placeId: string; location: PlaceLocation | null };

/**
 * Localização do local no Google, para o mapa da tela de detalhes.
 * `undefined` enquanto carrega; `null` se não der (sem chave, ID desconhecido, sem conexão):
 * nesse caso a tela só não mostra o mapa, o resto continua funcionando.
 */
export function usePlaceLocation(placeId: string): PlaceLocation | null | undefined {
  const configured = isGooglePlacesConfigured();
  // O resultado fica junto do ID: se o ID mudar, o resultado antigo não vale e volta a "carregando"
  const [result, setResult] = useState<Result | null>(null);

  useEffect(() => {
    if (!configured) return;
    const controller = new AbortController();
    getPlaceLocation(placeId, controller.signal)
      .then((location) => setResult({ placeId, location }))
      .catch(() => {
        if (!controller.signal.aborted) setResult({ placeId, location: null });
      });
    return () => controller.abort();
  }, [placeId, configured]);

  if (!configured) return null;
  return result?.placeId === placeId ? result.location : undefined;
}
