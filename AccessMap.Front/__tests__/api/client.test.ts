import type * as ClientModule from '../../src/api/client';
import type * as TokenStorageModule from '../../src/auth/tokenStorage';

// O BASE_URL é lido quando o módulo carrega: a env precisa estar definida antes do require
process.env.EXPO_PUBLIC_API_URL = 'http://api.teste';
const client: typeof ClientModule = require('../../src/api/client');
const { tokenStorage }: typeof TokenStorageModule = require('../../src/auth/tokenStorage');
const { ApiError, onSessionExpired, request } = client;

type FakeResponse = { ok: boolean; status: number; json: () => Promise<unknown>; text: () => Promise<string> };

function reply(status: number, body?: unknown): FakeResponse {
  const text = body === undefined ? '' : JSON.stringify(body);
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => JSON.parse(text),
    text: async () => text,
  };
}

const tokens = (suffix: string) => ({
  accessToken: `access-${suffix}`,
  refreshToken: `refresh-${suffix}`,
  tokenType: 'Bearer',
  expiresIn: 900,
  user: { id: 'u1', name: 'Pessoa' },
});

const fetchMock = jest.fn();

beforeEach(async () => {
  fetchMock.mockReset();
  global.fetch = fetchMock as unknown as typeof fetch;
  await tokenStorage.clear();
  onSessionExpired(null);
});

afterEach(() => {
  jest.useRealTimers();
});

describe('request', () => {
  it('monta a URL com a query e devolve o JSON', async () => {
    fetchMock.mockResolvedValue(reply(200, { ok: true }));

    const data = await request('/api/reviews', { query: { placeId: 'abc', page: 0, userId: undefined } });

    expect(data).toEqual({ ok: true });
    expect(fetchMock.mock.calls[0][0]).toBe('http://api.teste/api/reviews?placeId=abc&page=0');
  });

  it('manda o Bearer quando há access token, e não manda nas rotas de auth', async () => {
    tokenStorage.setAccessToken('token-atual');
    fetchMock.mockResolvedValue(reply(200, {}));

    await request('/api/users/me');
    await request('/api/auth/login', { method: 'POST', body: {}, auth: false });

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer token-atual');
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBeUndefined();
  });

  it('204 vira undefined', async () => {
    fetchMock.mockResolvedValue(reply(204));
    await expect(request('/api/reviews/1', { method: 'DELETE' })).resolves.toBeUndefined();
  });

  it('erro do backend vira ApiError com a mensagem e os campos', async () => {
    fetchMock.mockResolvedValue(reply(409, { status: 409, mensagem: 'Telefone já está em uso', campos: { phone: 'em uso' } }));

    const error = await request('/api/users/me', { method: 'PATCH', body: {} }).catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 409, message: 'Telefone já está em uso', fields: { phone: 'em uso' } });
  });

  it('erro sem corpo JSON vira "Erro inesperado"', async () => {
    fetchMock.mockResolvedValue({ ...reply(500), json: async () => Promise.reject(new Error('sem corpo')) });
    await expect(request('/api/x')).rejects.toMatchObject({ status: 500, message: 'Erro inesperado' });
  });

  it('falha de rede vira ApiError 0 com a mensagem de conexão', async () => {
    fetchMock.mockRejectedValue(new TypeError('Network request failed'));
    await expect(request('/api/x')).rejects.toMatchObject({
      status: 0,
      message: 'Não foi possível conectar ao servidor. Verifique sua conexão.',
    });
  });

  it('desiste depois de 15 s quando a API não responde', async () => {
    jest.useFakeTimers();
    // fetch que só termina quando o sinal de cancelamento dispara
    fetchMock.mockImplementation(
      (_url: string, init: { signal: AbortSignal }) =>
        new Promise((_resolve, reject) => {
          init.signal.addEventListener('abort', () => reject(Object.assign(new Error('aborted'), { name: 'AbortError' })));
        }),
    );

    const pending = request('/api/x');
    const assertion = expect(pending).rejects.toMatchObject({ status: 0 });
    jest.advanceTimersByTime(14_999);
    expect(fetchMock.mock.calls[0][1].signal.aborted).toBe(false);
    jest.advanceTimersByTime(1);
    await assertion;
  });
});

describe('refresh em 401', () => {
  it('renova o token uma vez só para várias requisições simultâneas e refaz cada uma', async () => {
    await tokenStorage.setRefreshToken('refresh-velho');
    tokenStorage.setAccessToken('access-velho');
    fetchMock.mockImplementation(async (url: string, init: { headers: Record<string, string> }) => {
      if (url.endsWith('/api/auth/refresh')) return reply(200, tokens('novo'));
      return init.headers.Authorization === 'Bearer access-novo' ? reply(200, { url }) : reply(401, { mensagem: 'expirado' });
    });

    const results = await Promise.all([request('/api/a'), request('/api/b'), request('/api/c')]);

    const refreshCalls = fetchMock.mock.calls.filter(([url]) => url.endsWith('/api/auth/refresh'));
    expect(refreshCalls).toHaveLength(1);
    expect(JSON.parse(refreshCalls[0][1].body)).toEqual({ refreshToken: 'refresh-velho' });
    expect(results).toEqual([{ url: 'http://api.teste/api/a' }, { url: 'http://api.teste/api/b' }, { url: 'http://api.teste/api/c' }]);
    expect(await tokenStorage.getRefreshToken()).toBe('refresh-novo');
  });

  it('refresh recusado limpa a sessão e avisa quem cuida do login', async () => {
    await tokenStorage.setRefreshToken('refresh-revogado');
    const expired = jest.fn();
    onSessionExpired(expired);
    fetchMock.mockResolvedValue(reply(401, { mensagem: 'Não autenticado' }));

    await expect(request('/api/users/me')).rejects.toMatchObject({ status: 401 });

    expect(expired).toHaveBeenCalledTimes(1);
    expect(await tokenStorage.getRefreshToken()).toBeNull();
  });

  it('sem refresh token guardado, nem tenta renovar', async () => {
    fetchMock.mockResolvedValue(reply(401, { mensagem: 'Não autenticado' }));

    await expect(request('/api/users/me')).rejects.toMatchObject({ status: 401 });

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
