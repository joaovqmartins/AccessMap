import { request } from './client';
import type { AdminReview, Paged } from './types';

const path = (id: string, action = '') => `/api/admin/reviews/${encodeURIComponent(id)}${action}`;

/** Só ADMIN: para os demais, a API responde 403. */
export const adminApi = {
  /** Fila de moderação, das mais antigas para as mais novas. */
  queue: ({ page = 0, size = 20 }: { page?: number; size?: number }) =>
    request<Paged<AdminReview>>('/api/admin/reviews', { query: { page, size } }),
  /** 409 se a avaliação não está oculta. */
  restore: (id: string) => request<void>(path(id, '/restore'), { method: 'POST' }),
  /** 409 se a avaliação não está aguardando moderação. */
  keep: (id: string) => request<void>(path(id, '/keep'), { method: 'POST' }),
  remove: (id: string) => request<void>(path(id), { method: 'DELETE' }),
};
