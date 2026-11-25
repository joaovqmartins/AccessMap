import { fireEvent, render, screen } from '@testing-library/react-native';

import Button from '../../src/components/Button';
import StarRatingInput from '../../src/components/StarRatingInput';
import TagAssessmentPicker from '../../src/components/TagAssessmentPicker';

describe('Button', () => {
  it('é um botão com o título como rótulo e chama o onPress', async () => {
    const onPress = jest.fn();
    await render(<Button title="Salvar" onPress={onPress} />);

    await fireEvent.press(screen.getByRole('button', { name: 'Salvar' }));

    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it('carregando: anuncia ocupado, fica desabilitado e não chama o onPress', async () => {
    const onPress = jest.fn();
    await render(<Button title="Salvar" onPress={onPress} loading />);

    const button = screen.getByRole('button', { name: 'Salvar' });
    await fireEvent.press(button);

    expect(onPress).not.toHaveBeenCalled();
    expect(button).toBeDisabled();
    expect(button).toBeBusy();
  });
});

describe('StarRatingInput', () => {
  it('cada estrela é um radio com rótulo e devolve a nota tocada', async () => {
    const onChange = jest.fn();
    await render(<StarRatingInput value={2} onChange={onChange} />);

    expect(screen.getByRole('radio', { name: '2 estrelas' })).toBeChecked();
    expect(screen.getByRole('radio', { name: '1 estrela' })).not.toBeChecked();

    await fireEvent.press(screen.getByRole('radio', { name: '4 estrelas' }));
    expect(onChange).toHaveBeenCalledWith(4);
  });

  it('mostra o erro', async () => {
    await render(<StarRatingInput value={0} onChange={jest.fn()} error="Escolha uma nota de 1 a 5" />);
    expect(screen.getByText('Escolha uma nota de 1 a 5')).toBeOnTheScreen();
  });
});

describe('TagAssessmentPicker', () => {
  it('marca uma avaliação para a característica', async () => {
    const onChange = jest.fn();
    await render(<TagAssessmentPicker value={{}} onChange={onChange} />);

    await fireEvent.press(screen.getByRole('radio', { name: 'Elevadores: Inadequado' }));

    expect(onChange).toHaveBeenCalledWith({ ELEVADORES: 'INADEQUADO' });
  });

  it('tocar de novo na opção marcada desmarca a característica', async () => {
    const onChange = jest.fn();
    await render(
      <TagAssessmentPicker value={{ ELEVADORES: 'INADEQUADO', RAMPAS_E_ENTRADAS: 'ADEQUADO' }} onChange={onChange} />,
    );

    expect(screen.getByRole('radio', { name: 'Elevadores: Inadequado' })).toBeChecked();
    await fireEvent.press(screen.getByRole('radio', { name: 'Elevadores: Inadequado' }));

    expect(onChange).toHaveBeenCalledWith({ RAMPAS_E_ENTRADAS: 'ADEQUADO' });
  });
});
