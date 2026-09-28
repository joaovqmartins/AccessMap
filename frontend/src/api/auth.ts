import { request } from './client';
import type { LoginRequest, RegisterRequest, TokenResponse, User } from './types';

export const authApi = {
  login: (body: LoginRequest) =>
    request<TokenResponse>('/api/auth/login', { method: 'POST', body, auth: false }),
  register: (body: RegisterRequest) =>
    request<TokenResponse>('/api/auth/register', { method: 'POST', body, auth: false }),
  logout: (refreshToken: string) =>
    request<void>('/api/auth/logout', { method: 'POST', body: { refreshToken }, auth: false }),
  me: () => request<User>('/api/users/me'),
};
