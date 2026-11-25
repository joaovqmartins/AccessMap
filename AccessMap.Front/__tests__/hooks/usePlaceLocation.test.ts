import { renderHook, waitFor } from '@testing-library/react-native';

import { getPlaceLocation, isGooglePlacesConfigured } from '../../src/api/googlePlaces';
import { usePlaceLocation } from '../../src/hooks/usePlaceLocation';

jest.mock('../../src/api/googlePlaces', () => ({
  getPlaceLocation: jest.fn(),
  isGooglePlacesConfigured: jest.fn(),
}));

const mockedGet = getPlaceLocation as jest.Mock;
const mockedConfigured = isGooglePlacesConfigured as jest.Mock;
const ibirapuera = { latitude: -23.61, longitude: -46.66, address: 'Av. Ibirapuera, 3103' };

beforeEach(() => {
  jest.clearAllMocks();
  mockedConfigured.mockReturnValue(true);
});

describe('usePlaceLocation', () => {
  it('começa carregando (undefined) e entrega a localização', async () => {
    mockedGet.mockResolvedValue(ibirapuera);
    const { result } = await renderHook(() => usePlaceLocation('p1'));

    await waitFor(() => expect(result.current).toEqual(ibirapuera));
    expect(mockedGet).toHaveBeenCalledWith('p1', expect.any(AbortSignal));
  });

  it('sem chave do Google, devolve null e nem chama a API', async () => {
    mockedConfigured.mockReturnValue(false);
    const { result } = await renderHook(() => usePlaceLocation('p1'));

    expect(result.current).toBeNull();
    expect(mockedGet).not.toHaveBeenCalled();
  });

  it('erro vira null: a tela segue sem o mapa', async () => {
    mockedGet.mockRejectedValue(new Error('rede'));
    const { result } = await renderHook(() => usePlaceLocation('p1'));

    await waitFor(() => expect(result.current).toBeNull());
  });

  it('trocar de local descarta o resultado anterior e volta a carregar', async () => {
    let resolveSecond: (value: unknown) => void = () => {};
    mockedGet
      .mockResolvedValueOnce(ibirapuera)
      .mockImplementationOnce(() => new Promise((resolve) => (resolveSecond = resolve)));

    const { result, rerender } = await renderHook(({ id }: { id: string }) => usePlaceLocation(id), {
      initialProps: { id: 'p1' },
    });
    await waitFor(() => expect(result.current).toEqual(ibirapuera));

    await rerender({ id: 'p2' });
    expect(result.current).toBeUndefined();

    const other = { latitude: 1, longitude: 2, address: 'Outro' };
    resolveSecond(other);
    await waitFor(() => expect(result.current).toEqual(other));
  });
});
