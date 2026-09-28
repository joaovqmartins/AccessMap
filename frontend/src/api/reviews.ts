import { request } from './client';
import type { Paged, Review } from './types';

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
};
