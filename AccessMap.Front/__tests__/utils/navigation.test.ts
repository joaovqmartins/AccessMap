import { router } from 'expo-router';

import {
  goBackOr,
  reviewFormPath,
  safeRedirect,
  setPendingRedirect,
  takePendingRedirect,
} from '../../src/utils/navigation';

jest.mock('expo-router', () => ({
  router: { back: jest.fn(), replace: jest.fn(), canGoBack: jest.fn() },
}));

const mockedRouter = router as jest.Mocked<typeof router>;

describe('safeRedirect', () => {
  it.each(['/locais/abc/avaliar', '/avaliacoes/123', '/locais/abc/avaliar?name=Padaria%20Central'])(
    'aceita o caminho interno %s',
    (path) => {
      expect(safeRedirect(path)).toBe(path);
    },
  );

  it.each([
    ['//exemplo.com', 'começa com // (outro domínio)'],
    ['https://exemplo.com', 'tem esquema'],
    ['/x?volta=https://exemplo.com', 'esconde um esquema no meio'],
    ['/\\exemplo.com', 'tem barra invertida'],
    ['locais/abc', 'não começa com barra'],
  ])('recusa %s (%s)', (path) => {
    expect(safeRedirect(path)).toBeNull();
  });

  it('recusa o que não é texto (parâmetro repetido vira array)', () => {
    expect(safeRedirect(['/a', '/b'])).toBeNull();
    expect(safeRedirect(undefined)).toBeNull();
  });
});

describe('redirect pendente', () => {
  it('é lido uma vez só', () => {
    setPendingRedirect('/perfil');
    expect(takePendingRedirect()).toBe('/perfil');
    expect(takePendingRedirect()).toBeNull();
  });
});

describe('reviewFormPath', () => {
  it('codifica o Place ID e o nome', () => {
    expect(reviewFormPath('ChIJ/x', 'Café & Bar')).toBe('/locais/ChIJ%2Fx/avaliar?name=Caf%C3%A9%20%26%20Bar');
  });

  it('sem nome, não põe query', () => {
    expect(reviewFormPath('abc')).toBe('/locais/abc/avaliar');
  });
});

describe('goBackOr', () => {
  beforeEach(() => jest.clearAllMocks());

  it('volta quando há tela anterior', () => {
    mockedRouter.canGoBack.mockReturnValue(true);
    goBackOr('/');
    expect(mockedRouter.back).toHaveBeenCalled();
    expect(mockedRouter.replace).not.toHaveBeenCalled();
  });

  it('sem histórico, vai para o destino de reserva', () => {
    mockedRouter.canGoBack.mockReturnValue(false);
    goBackOr('/perfil');
    expect(mockedRouter.replace).toHaveBeenCalledWith('/perfil');
    expect(mockedRouter.back).not.toHaveBeenCalled();
  });
});
