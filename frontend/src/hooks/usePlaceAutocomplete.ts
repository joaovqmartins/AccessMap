import { randomUUID } from 'expo-crypto';
import { useCallback, useEffect, useRef, useState } from 'react';

import { autocomplete, type PlaceSuggestion } from '../api/googlePlaces';

// Recomendação do Google para custo: esperar uma pausa na digitação e alguns caracteres.
const DEBOUNCE_MS = 300;
export const MIN_QUERY_LENGTH = 3;

/** Resultado da última busca concluída, com o texto que a originou. */
type Result = {
  input: string;
  suggestions: PlaceSuggestion[];
  error: string | null;
};

/**
 * Sugestões do Google Places para `query`, com debounce e cancelamento da busca anterior.
 * `endSession` deve ser chamado quando o usuário escolhe um local: a próxima digitação abre
 * uma nova sessão de cobrança.
 */
export function usePlaceAutocomplete(query: string) {
  const [result, setResult] = useState<Result | null>(null);
  const sessionToken = useRef<string | null>(null);

  const input = query.trim();
  const active = input.length >= MIN_QUERY_LENGTH;

  useEffect(() => {
    if (!active) return;

    const controller = new AbortController();
    const timer = setTimeout(async () => {
      sessionToken.current ??= randomUUID();
      try {
        const suggestions = await autocomplete(input, sessionToken.current, controller.signal);
        setResult({ input, suggestions, error: null });
      } catch (e) {
        if (controller.signal.aborted) return;
        setResult({ input, suggestions: [], error: e instanceof Error ? e.message : 'Erro na busca' });
      }
    }, DEBOUNCE_MS);

    // Nova digitação: cancela o timer e a requisição em andamento, para não mostrar resultado velho
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [active, input]);

  const endSession = useCallback(() => {
    sessionToken.current = null;
  }, []);

  // Só vale o resultado do texto atual; enquanto ele não chega, está carregando
  const current = active && result?.input === input ? result : null;
  return {
    suggestions: current?.suggestions ?? [],
    error: current?.error ?? null,
    loading: active && !current,
    endSession,
  };
}
