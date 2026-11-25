import { fireEvent, render, screen } from '@testing-library/react-native';
import { router } from 'expo-router';

import { ApiError } from '../../src/api/client';
import { reviewsApi } from '../../src/api/reviews';
import type { Review } from '../../src/api/types';
import { useAuth } from '../../src/auth/AuthContext';
import ReviewFormScreen from '../../src/screens/ReviewFormScreen';

jest.mock('expo-router', () => ({
  Stack: { Screen: () => null },
  router: { replace: jest.fn(), back: jest.fn(), canGoBack: jest.fn(() => true) },
}));
jest.mock('../../src/auth/AuthContext', () => ({ useAuth: jest.fn() }));
jest.mock('../../src/api/reviews', () => ({
  reviewsApi: { findMine: jest.fn(), create: jest.fn(), get: jest.fn(), update: jest.fn() },
}));

const api = reviewsApi as jest.Mocked<typeof reviewsApi>;
const mockedRouter = router as jest.Mocked<typeof router>;

function existing(overrides: Partial<Review> = {}): Review {
  return {
    id: 'r1',
    placeId: 'p1',
    rating: 3,
    comment: 'Antes',
    tags: { RAMPAS_E_ENTRADAS: 'ADEQUADO' },
    status: 'PUBLICADA',
    createdAt: '2026-11-25T15:00:00Z',
    updatedAt: '2026-11-25T15:00:00Z',
    mine: true,
    ...overrides,
  } as Review;
}

beforeEach(() => {
  jest.clearAllMocks();
  mockedRouter.canGoBack.mockReturnValue(true);
  (useAuth as jest.Mock).mockReturnValue({ user: { id: 'u1' } });
});

describe('ReviewFormScreen — criar', () => {
  it('quem já avaliou o local vai direto para a própria avaliação, com o aviso', async () => {
    api.findMine.mockResolvedValue(existing());
    await render(<ReviewFormScreen mode="create" placeId="p1" placeName="Padaria" />);

    expect(api.findMine).toHaveBeenCalledWith('p1', 'u1');
    expect(mockedRouter.replace).toHaveBeenCalledWith({
      pathname: '/avaliacoes/[id]',
      params: { id: 'r1', jaAvaliado: '1' },
    });
  });

  it('valida nota e característica antes de enviar', async () => {
    api.findMine.mockResolvedValue(null);
    await render(<ReviewFormScreen mode="create" placeId="p1" />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Publicar avaliação' }));

    expect(screen.getByText('Escolha uma nota de 1 a 5')).toBeOnTheScreen();
    expect(screen.getByText('Avalie ao menos uma característica de acessibilidade')).toBeOnTheScreen();
    expect(api.create).not.toHaveBeenCalled();
  });

  it('publica com o nome do local e volta', async () => {
    api.findMine.mockResolvedValue(null);
    api.create.mockResolvedValue(existing());
    await render(<ReviewFormScreen mode="create" placeId="p1" placeName="Padaria Central" />);

    await fireEvent.press(await screen.findByRole('radio', { name: '4 estrelas' }));
    await fireEvent.press(screen.getByRole('radio', { name: 'Elevadores: Adequado' }));
    await fireEvent.changeText(screen.getByLabelText('Comentário (opcional)'), '  Elevador amplo  ');
    await fireEvent.press(screen.getByRole('button', { name: 'Publicar avaliação' }));

    expect(api.create).toHaveBeenCalledWith({
      placeId: 'p1',
      placeName: 'Padaria Central',
      rating: 4,
      comment: 'Elevador amplo',
      tags: { ELEVADORES: 'ADEQUADO' },
    });
    expect(mockedRouter.back).toHaveBeenCalled();
  });

  it('sem histórico para voltar, vai para o local avaliado', async () => {
    api.findMine.mockResolvedValue(null);
    api.create.mockResolvedValue(existing());
    mockedRouter.canGoBack.mockReturnValue(false);
    await render(<ReviewFormScreen mode="create" placeId="p1" placeName="Padaria" />);

    await fireEvent.press(await screen.findByRole('radio', { name: '5 estrelas' }));
    await fireEvent.press(screen.getByRole('radio', { name: 'Atendimento: Adequado' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Publicar avaliação' }));

    expect(mockedRouter.replace).toHaveBeenCalledWith({
      pathname: '/locais/[placeId]',
      params: { placeId: 'p1', name: 'Padaria' },
    });
  });

  it('409 no envio (avaliou em outra aba) leva à avaliação existente', async () => {
    api.findMine.mockResolvedValueOnce(null).mockResolvedValueOnce(existing({ id: 'r9' }));
    api.create.mockRejectedValue(new ApiError(409, 'Você já avaliou este local.'));
    await render(<ReviewFormScreen mode="create" placeId="p1" />);

    await fireEvent.press(await screen.findByRole('radio', { name: '2 estrelas' }));
    await fireEvent.press(screen.getByRole('radio', { name: 'Outros: Inexistente' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Publicar avaliação' }));

    expect(mockedRouter.replace).toHaveBeenCalledWith({
      pathname: '/avaliacoes/[id]',
      params: { id: 'r9', jaAvaliado: '1' },
    });
  });

  it('sem conseguir conferir se já avaliou, mostra o formulário mesmo assim', async () => {
    api.findMine.mockRejectedValue(new ApiError(0, 'sem conexão'));
    await render(<ReviewFormScreen mode="create" placeId="p1" />);

    expect(await screen.findByRole('button', { name: 'Publicar avaliação' })).toBeOnTheScreen();
  });
});

describe('ReviewFormScreen — editar', () => {
  it('manda só o que mudou', async () => {
    api.get.mockResolvedValue(existing());
    api.update.mockResolvedValue(existing());
    await render(<ReviewFormScreen mode="edit" reviewId="r1" />);

    await fireEvent.press(await screen.findByRole('radio', { name: '5 estrelas' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(api.update).toHaveBeenCalledWith('r1', { rating: 5 });
  });

  it('não deixa editar avaliação de outra pessoa', async () => {
    api.get.mockResolvedValue(existing({ mine: false }));
    await render(<ReviewFormScreen mode="edit" reviewId="r1" />);

    expect(await screen.findByText('Você só pode editar as suas próprias avaliações.')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Salvar alterações' })).toBeNull();
  });

  it('não deixa editar avaliação em análise', async () => {
    api.get.mockResolvedValue(existing({ status: 'OCULTA' }));
    await render(<ReviewFormScreen mode="edit" reviewId="r1" />);

    expect(await screen.findByText('Esta avaliação está em análise e não pode ser editada.')).toBeOnTheScreen();
  });
});
