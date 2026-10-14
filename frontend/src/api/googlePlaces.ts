import { ApiError } from './client';

// Places API (New). A chave vai dentro do app (EXPO_PUBLIC_*): no Google Cloud ela fica
// restrita à Places API e com cota baixa. Veja o README, "Chave da Google Maps Platform".
const API_KEY = process.env.EXPO_PUBLIC_GOOGLE_PLACES_API_KEY ?? '';
const AUTOCOMPLETE_URL = 'https://places.googleapis.com/v1/places:autocomplete';

// Só os campos usados: o Google cobra e responde de acordo com o que é pedido.
const FIELD_MASK = 'suggestions.placePrediction.placeId,suggestions.placePrediction.structuredFormat';

export interface PlaceSuggestion {
  /** Place ID do Google: é a chave do local no backend. */
  placeId: string;
  mainText: string;
  secondaryText: string;
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
  let response: Response;
  try {
    response = await fetch(AUTOCOMPLETE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Goog-Api-Key': API_KEY,
        'X-Goog-FieldMask': FIELD_MASK,
      },
      body: JSON.stringify({
        input,
        sessionToken,
        languageCode: 'pt-BR',
        regionCode: 'br',
        includedRegionCodes: ['br'],
      }),
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

  const data = (await response.json()) as AutocompleteResponse;
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
