import { ApiError, request } from './client';
import type { Place } from './types';

export const placesApi = {
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
