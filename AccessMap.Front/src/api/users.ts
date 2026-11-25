import { request } from './client';
import type { User, UserUpdateRequest } from './types';

export const usersApi = {
  me: () => request<User>('/api/users/me'),
  updateMe: (body: UserUpdateRequest) => request<User>('/api/users/me', { method: 'PATCH', body }),
  /** Exige a senha atual: errada ou ausente, 400. As avaliações ficam publicadas e anônimas. */
  deleteMe: (currentPassword: string) =>
    request<void>('/api/users/me', { method: 'DELETE', body: { currentPassword } }),
};
