import { request } from './client';
import type { Paged, Review, ReviewRequest, ReviewUpdateRequest } from './types';

type ListParams = {
  placeId?: string;
  userId?: string;
  page?: number;
  size?: number;
};

export const reviewsApi = {
  /** Paginado, das mais recentes para as mais antigas. */
  list: ({ placeId, userId, page = 0, size = 20 }: ListParams) =>
    request<Paged<Review>>('/api/reviews', { query: { placeId, userId, page, size } }),
  get: (id: string) => request<Review>(`/api/reviews/${encodeURIComponent(id)}`),
  /** 409 se o usuário já avaliou o local. */
  create: (body: ReviewRequest) => request<Review>('/api/reviews', { method: 'POST', body }),
  update: (id: string, body: ReviewUpdateRequest) =>
    request<Review>(`/api/reviews/${encodeURIComponent(id)}`, { method: 'PATCH', body }),
  remove: (id: string) => request<void>(`/api/reviews/${encodeURIComponent(id)}`, { method: 'DELETE' }),

  /** A avaliação publicada do usuário para o local, se houver (no máximo uma por local). */
  async findMine(placeId: string, userId: string): Promise<Review | null> {
    const page = await reviewsApi.list({ placeId, userId, size: 1 });
    return page.content[0] ?? null;
  },
};
