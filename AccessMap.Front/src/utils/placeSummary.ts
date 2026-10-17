import type { PlaceSummary } from '../api/types';

/** Características em que a maioria estrita dos votos é ADEQUADO (mesma regra da busca do backend). */
export function adequateTagCount(summary: PlaceSummary) {
  return Object.values(summary.tagStats).filter(
    (s) => s && s.adequadoCount > s.inadequadoCount + s.inexistenteCount,
  ).length;
}
