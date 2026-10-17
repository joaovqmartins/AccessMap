import { ApiError, request } from './client';
import type { Place, PlaceBatchRequest, PlaceSummary } from './types';

export const placesApi = {
  /** Agregados de vários locais numa chamada só, na mesma ordem de `placeIds`. */
  batch(body: PlaceBatchRequest): Promise<PlaceSummary[]> {
    return request<PlaceSummary[]>('/api/places/batch', { method: 'POST', body });
  },


  /** O backend responde 404 enquanto o local não tem nenhuma avaliação: aqui isso vira `null`. */
  async get(placeId: string): Promise<Place | null> {
    try {
      return await request<Place>(`/api/places/${encodeURIComponent(placeId)}`);
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) return null;
      throw e;
    }
  },
};
