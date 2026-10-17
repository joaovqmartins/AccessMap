import { ApiError } from './client';

// Places API (New). A chave vai dentro do app (EXPO_PUBLIC_*): no Google Cloud ela fica
// restrita à Places API e com cota baixa. Veja o README, "Chave da Google Maps Platform".
const API_KEY = process.env.EXPO_PUBLIC_GOOGLE_PLACES_API_KEY ?? '';
const AUTOCOMPLETE_URL = 'https://places.googleapis.com/v1/places:autocomplete';
const NEARBY_URL = 'https://places.googleapis.com/v1/places:searchNearby';

// Só os campos usados: o Google cobra e responde de acordo com o que é pedido.
const AUTOCOMPLETE_FIELD_MASK = 'suggestions.placePrediction.placeId,suggestions.placePrediction.structuredFormat';
const NEARBY_FIELD_MASK = 'places.id,places.displayName,places.shortFormattedAddress,places.location';

/** Raio padrão da busca por proximidade, em metros (o Google aceita até 50 km). */
export const NEARBY_RADIUS_M = 5000;

export interface PlaceSuggestion {
  /** Place ID do Google: é a chave do local no backend. */
  placeId: string;
  mainText: string;
  secondaryText: string;
}

export interface NearbyPlace {
  placeId: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
}

type AutocompleteResponse = {
  suggestions?: {
    placePrediction?: {
      placeId: string;
      structuredFormat?: {
        mainText?: { text: string };
        secondaryText?: { text: string };
      };
    };
  }[];
};

type NearbyResponse = {
  places?: {
    id: string;
    displayName?: { text: string };
    shortFormattedAddress?: string;
    location?: { latitude: number; longitude: number };
  }[];
};

export function isGooglePlacesConfigured() {
  return API_KEY.trim().length > 0;
}

/**
 * Sugestões de locais no Brasil para o texto digitado.
 * `sessionToken` agrupa as buscas de uma mesma digitação numa sessão de cobrança do Google.
 */
export async function autocomplete(
  input: string,
  sessionToken: string,
  signal?: AbortSignal,
): Promise<PlaceSuggestion[]> {
  const data = await post<AutocompleteResponse>(
    AUTOCOMPLETE_URL,
    AUTOCOMPLETE_FIELD_MASK,
    {
      input,
      sessionToken,
      languageCode: 'pt-BR',
      regionCode: 'br',
      includedRegionCodes: ['br'],
    },
    signal,
  );
  return (data.suggestions ?? []).flatMap(({ placePrediction: p }) =>
    p
      ? [{
          placeId: p.placeId,
          mainText: p.structuredFormat?.mainText?.text ?? '',
          secondaryText: p.structuredFormat?.secondaryText?.text ?? '',
        }]
      : [],
  );
}

/**
 * Até 20 locais dos tipos pedidos em volta de `center`, do mais perto para o mais longe.
 * `includedTypes` são tipos da Tabela A do Google (ex.: `restaurant`); vazio traz qualquer tipo.
 */
export async function searchNearby(
  center: { latitude: number; longitude: number },
  includedTypes: readonly string[],
  signal?: AbortSignal,
): Promise<NearbyPlace[]> {
  const data = await post<NearbyResponse>(
    NEARBY_URL,
    NEARBY_FIELD_MASK,
    {
      includedTypes: includedTypes.length ? includedTypes : undefined,
      maxResultCount: 20,
      rankPreference: 'DISTANCE',
      locationRestriction: { circle: { center, radius: NEARBY_RADIUS_M } },
      languageCode: 'pt-BR',
      regionCode: 'br',
    },
    signal,
  );
  return (data.places ?? []).flatMap((p) =>
    p.location
      ? [{
          placeId: p.id,
          name: p.displayName?.text ?? '',
          address: p.shortFormattedAddress ?? '',
          latitude: p.location.latitude,
          longitude: p.location.longitude,
        }]
      : [],
  );
}

async function post<T>(url: string, fieldMask: string, body: unknown, signal?: AbortSignal): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Goog-Api-Key': API_KEY,
        'X-Goog-FieldMask': fieldMask,
      },
      body: JSON.stringify(body),
      signal,
    });
  } catch (e) {
    if (e instanceof Error && e.name === 'AbortError') throw e;
    throw new ApiError(0, 'Não foi possível buscar locais. Verifique sua conexão.');
  }

  if (!response.ok) {
    // 400/403 quase sempre é chave inválida, sem a Places API (New) ativada ou com restrição errada
    throw new ApiError(
      response.status,
      response.status === 400 || response.status === 403
        ? 'Busca de locais indisponível: verifique a chave do Google.'
        : 'Não foi possível buscar locais agora. Tente novamente.',
    );
  }

  return (await response.json()) as T;
}
