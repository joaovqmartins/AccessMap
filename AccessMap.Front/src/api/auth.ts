import { request } from './client';
import type { LoginRequest, RegisterRequest, TokenResponse } from './types';

export const authApi = {
  login: (body: LoginRequest) =>
    request<TokenResponse>('/api/auth/login', { method: 'POST', body, auth: false }),
  register: (body: RegisterRequest) =>
    request<TokenResponse>('/api/auth/register', { method: 'POST', body, auth: false }),
  logout: (refreshToken: string) =>
    request<void>('/api/auth/logout', { method: 'POST', body: { refreshToken }, auth: false }),
};

export const passwordResetApi = {
  /** Responde igual exista ou não conta com o e-mail (#109). */
  request: (email: string) =>
    request<{ message: string }>('/api/auth/password-reset/request', { method: 'POST', body: { email }, auth: false }),
  confirm: (body: { email: string; code: string; password: string }) =>
    request<{ message: string }>('/api/auth/password-reset/confirm', { method: 'POST', body, auth: false }),
};
