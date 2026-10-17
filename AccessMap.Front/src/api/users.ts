import { request } from './client';
import type { User, UserUpdateRequest } from './types';

export const usersApi = {
  me: () => request<User>('/api/users/me'),
  updateMe: (body: UserUpdateRequest) => request<User>('/api/users/me', { method: 'PATCH', body }),
};
