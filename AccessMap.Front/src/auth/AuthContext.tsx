import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import { authApi } from '../api/auth';
import { onSessionExpired, refreshSession, saveTokens } from '../api/client';
import { usersApi } from '../api/users';
import type { LoginRequest, RegisterRequest, User } from '../api/types';
import { tokenStorage } from './tokenStorage';

type AuthStatus = 'loading' | 'signedIn' | 'signedOut';

type AuthContextValue = {
  status: AuthStatus;
  user: User | null;
  signIn: (body: LoginRequest) => Promise<void>;
  signUp: (body: RegisterRequest) => Promise<void>;
  signOut: () => Promise<void>;
  /** Recarrega /users/me (ou aplica o usuário já devolvido por um PATCH). */
  refreshUser: (updated?: User) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<User | null>(null);

  const reset = useCallback(async () => {
    await tokenStorage.clear();
    setUser(null);
    setStatus('signedOut');
  }, []);

  // Restaura a sessão a partir do refresh token guardado no aparelho
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const tokens = await refreshSession();
        if (cancelled) return;
        if (tokens) {
          setUser(tokens.user);
          setStatus('signedIn');
        } else {
          setStatus('signedOut');
        }
      } catch {
        // Sem conexão no boot: volta ao login sem apagar o refresh token
        if (!cancelled) setStatus('signedOut');
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    onSessionExpired(() => {
      setUser(null);
      setStatus('signedOut');
    });
    return () => onSessionExpired(null);
  }, []);

  const signIn = useCallback(async (body: LoginRequest) => {
    const tokens = await authApi.login(body);
    await saveTokens(tokens);
    setUser(tokens.user);
    setStatus('signedIn');
  }, []);

  const signUp = useCallback(async (body: RegisterRequest) => {
    const tokens = await authApi.register(body);
    await saveTokens(tokens);
    setUser(tokens.user);
    setStatus('signedIn');
  }, []);

  const signOut = useCallback(async () => {
    const refreshToken = await tokenStorage.getRefreshToken();
    if (refreshToken) {
      // Logout é idempotente no backend; se falhar (offline), a sessão local é encerrada mesmo assim
      await authApi.logout(refreshToken).catch(() => undefined);
    }
    await reset();
  }, [reset]);

  const refreshUser = useCallback(async (updated?: User) => {
    setUser(updated ?? (await usersApi.me()));
  }, []);

  const value = useMemo(
    () => ({ status, user, signIn, signUp, signOut, refreshUser }),
    [status, user, signIn, signUp, signOut, refreshUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth deve ser usado dentro de <AuthProvider>');
  return context;
}
