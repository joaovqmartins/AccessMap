const REFRESH_TOKEN_KEY = 'accessmap.refreshToken';

// Versão web (expo start --web, só em desenvolvimento): expo-secure-store não existe no navegador.
// O refresh token vai para o sessionStorage, que some ao fechar a aba, em vez do localStorage.
let accessToken: string | null = null;

export const tokenStorage = {
  getAccessToken: () => accessToken,
  setAccessToken: (token: string | null) => {
    accessToken = token;
  },
  getRefreshToken: async () => sessionStorage.getItem(REFRESH_TOKEN_KEY),
  setRefreshToken: async (token: string) => sessionStorage.setItem(REFRESH_TOKEN_KEY, token),
  async clear() {
    accessToken = null;
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};
