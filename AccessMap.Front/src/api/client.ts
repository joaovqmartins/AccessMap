import { tokenStorage } from '../auth/tokenStorage';
import { resolveApiBaseUrl } from './baseUrl';
import type { TokenResponse } from './types';

const BASE_URL = resolveApiBaseUrl();

// Sem limite, uma API fora do ar deixa o botão girando até o sistema desistir da conexão (dezenas de segundos)
const REQUEST_TIMEOUT_MS = 15_000;
const CONNECTION_ERROR = 'Não foi possível conectar ao servidor. Verifique sua conexão.';

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly fields: Record<string, string> = {},
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE';
  body?: unknown;
  query?: Record<string, string | number | undefined>;
  /** Envia o Bearer e tenta refresh em 401. Desligado para os endpoints de /api/auth. */
  auth?: boolean;
};

let sessionExpiredHandler: (() => void) | null = null;

/** Chamado quando o refresh falha: o AuthContext usa para voltar ao login. */
export function onSessionExpired(handler: (() => void) | null) {
  sessionExpiredHandler = handler;
}

// Refresh token é rotativo: reapresentar um já usado derruba todas as sessões.
// Por isso requisições concorrentes que recebem 401 compartilham um único refresh.
let refreshInFlight: Promise<TokenResponse | null> | null = null;

export function refreshSession(): Promise<TokenResponse | null> {
  if (!refreshInFlight) {
    refreshInFlight = doRefresh().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

async function doRefresh(): Promise<TokenResponse | null> {
  const refreshToken = await tokenStorage.getRefreshToken();
  if (!refreshToken) return null;
  try {
    const tokens = await request<TokenResponse>('/api/auth/refresh', {
      method: 'POST',
      body: { refreshToken },
      auth: false,
    });
    await saveTokens(tokens);
    return tokens;
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      await tokenStorage.clear();
      return null;
    }
    throw e;
  }
}

export async function saveTokens(tokens: TokenResponse) {
  tokenStorage.setAccessToken(tokens.accessToken);
  await tokenStorage.setRefreshToken(tokens.refreshToken);
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { auth = true } = options;
  let response = await send(path, options, auth);

  if (response.status === 401 && auth) {
    const tokens = await refreshSession();
    if (!tokens) {
      sessionExpiredHandler?.();
      throw await toApiError(response);
    }
    response = await send(path, options, auth);
  }

  if (!response.ok) throw await toApiError(response);
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

function send(path: string, { method = 'GET', body, query }: RequestOptions, auth: boolean) {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = auth ? tokenStorage.getAccessToken() : null;
  if (token) headers.Authorization = `Bearer ${token}`;

  // AbortController + setTimeout em vez de AbortSignal.timeout: funciona igual no navegador e no Hermes
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  return fetch(BASE_URL + path + toQueryString(query), {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    signal: controller.signal,
  })
    .catch(() => {
      throw new ApiError(0, CONNECTION_ERROR);
    })
    .finally(() => clearTimeout(timer));
}

function toQueryString(query?: RequestOptions['query']) {
  if (!query) return '';
  const params = Object.entries(query)
    .filter(([, v]) => v !== undefined && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`);
  return params.length ? `?${params.join('&')}` : '';
}

// Backend responde erros como { status, mensagem, campos? } (ApiExceptionHandler)
async function toApiError(response: Response): Promise<ApiError> {
  try {
    const data = await response.json();
    return new ApiError(response.status, data.mensagem ?? 'Erro inesperado', data.campos ?? {});
  } catch {
    return new ApiError(response.status, 'Erro inesperado');
  }
}
