import type * as GooglePlacesModule from '../../src/api/googlePlaces';

// A chave é lida quando o módulo carrega
process.env.EXPO_PUBLIC_GOOGLE_PLACES_API_KEY = 'chave-de-teste';
const google: typeof GooglePlacesModule = require('../../src/api/googlePlaces');

function reply(status: number, body: unknown = {}) {
  return { ok: status >= 200 && status < 300, status, json: async () => body };
}

const fetchMock = jest.fn();

beforeEach(() => {
  fetchMock.mockReset();
  global.fetch = fetchMock as unknown as typeof fetch;
});

describe('isGooglePlacesConfigured', () => {
  it('é verdadeiro com a chave definida', () => {
    expect(google.isGooglePlacesConfigured()).toBe(true);
  });
});

describe('autocomplete', () => {
  it('manda a chave, o field mask e a sessão, e achata as sugestões', async () => {
    fetchMock.mockResolvedValue(
      reply(200, {
        suggestions: [
          {
            placePrediction: {
              placeId: 'p1',
              structuredFormat: { mainText: { text: 'Padaria Central' }, secondaryText: { text: 'São Paulo - SP' } },
            },
          },
          {}, // sugestão de consulta, sem placePrediction: fica de fora
        ],
      }),
    );

    const suggestions = await google.autocomplete('padaria', 'sessao-1');

    expect(suggestions).toEqual([{ placeId: 'p1', mainText: 'Padaria Central', secondaryText: 'São Paulo - SP' }]);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('https://places.googleapis.com/v1/places:autocomplete');
    expect(init.method).toBe('POST');
    expect(init.headers['X-Goog-Api-Key']).toBe('chave-de-teste');
    expect(JSON.parse(init.body)).toMatchObject({ input: 'padaria', sessionToken: 'sessao-1', regionCode: 'br' });
  });

  it('chave recusada vira a mensagem de verificar a chave', async () => {
    fetchMock.mockResolvedValue(reply(403));
    await expect(google.autocomplete('x', 's')).rejects.toMatchObject({
      status: 403,
      message: 'Busca de locais indisponível: verifique a chave do Google.',
    });
  });
});

describe('searchNearby', () => {
  it('descarta locais sem coordenadas', async () => {
    fetchMock.mockResolvedValue(
      reply(200, {
        places: [
          { id: 'a', displayName: { text: 'Com local' }, shortFormattedAddress: 'Rua 1', location: { latitude: 1, longitude: 2 } },
          { id: 'b', displayName: { text: 'Sem local' } },
        ],
      }),
    );

    const places = await google.searchNearby({ latitude: 0, longitude: 0 }, ['restaurant']);

    expect(places).toEqual([{ placeId: 'a', name: 'Com local', address: 'Rua 1', latitude: 1, longitude: 2 }]);
  });
});

describe('getPlaceLocation', () => {
  it('faz um GET com o Place ID e devolve coordenadas e endereço', async () => {
    fetchMock.mockResolvedValue(
      reply(200, { location: { latitude: -23.61, longitude: -46.66 }, formattedAddress: 'Av. Ibirapuera, 3103' }),
    );

    const location = await google.getPlaceLocation('ChIJ/abc');

    expect(location).toEqual({ latitude: -23.61, longitude: -46.66, address: 'Av. Ibirapuera, 3103' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toMatch(/^https:\/\/places\.googleapis\.com\/v1\/places\/ChIJ%2Fabc\?/);
    expect(init.method).toBe('GET');
    expect(init.body).toBeUndefined();
    expect(init.headers['X-Goog-FieldMask']).toBe('location,formattedAddress');
  });

  it.each([400, 404])('Place ID desconhecido (%d) vira null, para a tela seguir sem mapa', async (status) => {
    fetchMock.mockResolvedValue(reply(status));
    await expect(google.getPlaceLocation('e2e-falso')).resolves.toBeNull();
  });

  it('resposta sem location vira null', async () => {
    fetchMock.mockResolvedValue(reply(200, { formattedAddress: 'Só endereço' }));
    await expect(google.getPlaceLocation('x')).resolves.toBeNull();
  });

  it('outros erros sobem para quem chamou', async () => {
    fetchMock.mockResolvedValue(reply(500));
    await expect(google.getPlaceLocation('x')).rejects.toMatchObject({ status: 500 });
  });
});
