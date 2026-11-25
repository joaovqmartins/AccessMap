import { fireEvent, render, screen } from '@testing-library/react-native';

import type { Review } from '../../src/api/types';
import ReviewCard from '../../src/components/ReviewCard';

function review(overrides: Partial<Review> = {}): Review {
  return {
    id: 'r1',
    placeId: 'p1',
    placeName: 'Padaria Central',
    rating: 4,
    comment: 'Rampa boa',
    tags: { RAMPAS_E_ENTRADAS: 'ADEQUADO', ELEVADORES: 'INEXISTENTE' },
    status: 'PUBLICADA',
    createdAt: '2026-11-25T15:00:00Z',
    updatedAt: '2026-11-25T15:00:00Z',
    mine: false,
    ...overrides,
  } as Review;
}

describe('ReviewCard', () => {
  it('mostra autor anônimo, comentário e cada característica com a avaliação em texto', async () => {
    await render(<ReviewCard review={review()} />);

    expect(screen.getByText(/^Usuário · /)).toBeOnTheScreen();
    expect(screen.getByText('Rampa boa')).toBeOnTheScreen();
    expect(screen.getByText(/Rampas e entradas:/)).toHaveTextContent('Rampas e entradas: Adequado');
    expect(screen.getByText(/Elevadores:/)).toHaveTextContent('Elevadores: Inexistente');
  });

  it('a própria avaliação aparece como "Sua avaliação", com as necessidades declaradas', async () => {
    await render(<ReviewCard review={review({ mine: true, reviewerNeeds: ['MOBILIDADE_REDUZIDA'] })} isMine />);

    expect(screen.getByText(/^Sua avaliação · /)).toBeOnTheScreen();
    expect(screen.getByText('Avaliado por alguém com: mobilidade reduzida')).toBeOnTheScreen();
  });

  it('mostra o selo da conta quando a API manda', async () => {
    await render(<ReviewCard review={review({ trustBadge: 'CONFIAVEL' })} />);
    expect(screen.getByText('✓ Avaliador confiável')).toBeOnTheScreen();
  });

  it('com showPlace, mostra o nome do local ou o aviso de local sem nome', async () => {
    const { rerender } = await render(<ReviewCard review={review()} showPlace />);
    expect(screen.getByRole('header', { name: 'Padaria Central' })).toBeOnTheScreen();

    await rerender(<ReviewCard review={review({ placeName: null })} showPlace />);
    expect(screen.getByText('Local sem nome salvo')).toBeOnTheScreen();
  });

  it('sem onPress não é botão; com onPress abre os detalhes', async () => {
    const { rerender } = await render(<ReviewCard review={review()} />);
    expect(screen.queryByRole('button')).toBeNull();

    const onPress = jest.fn();
    await rerender(<ReviewCard review={review()} onPress={onPress} />);
    await fireEvent.press(screen.getByRole('button'));
    expect(onPress).toHaveBeenCalled();
  });
});
