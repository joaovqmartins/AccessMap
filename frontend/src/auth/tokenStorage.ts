import * as SecureStore from 'expo-secure-store';

const REFRESH_TOKEN_KEY = 'accessmap.refreshToken';

// O access token (15 min) fica só em memória; o refresh token vai para o armazenamento
// seguro do aparelho para manter a sessão entre aberturas do app.
let accessToken: string | null = null;

export const tokenStorage = {
  getAccessToken: () => accessToken,
  setAccessToken: (token: string | null) => {
    accessToken = token;
  },
  getRefreshToken: () => SecureStore.getItemAsync(REFRESH_TOKEN_KEY),
  setRefreshToken: (token: string) => SecureStore.setItemAsync(REFRESH_TOKEN_KEY, token),
  async clear() {
    accessToken = null;
    await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
  },
};
