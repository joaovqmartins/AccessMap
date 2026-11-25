import { fireEvent, render, screen } from '@testing-library/react-native';
import { router } from 'expo-router';

import { useAuth } from '../../src/auth/AuthContext';
import MainMenu from '../../src/components/MainMenu';
import { confirm } from '../../src/utils/dialog';

jest.mock('expo-router', () => ({ router: { push: jest.fn() } }));
jest.mock('../../src/auth/AuthContext', () => ({ useAuth: jest.fn() }));
jest.mock('../../src/utils/dialog', () => ({ confirm: jest.fn() }));

const mockedUseAuth = useAuth as jest.Mock;
const mockedConfirm = confirm as jest.Mock;
const signOut = jest.fn();

function signedInAs(role: 'USER' | 'ADMIN') {
  mockedUseAuth.mockReturnValue({ user: { id: 'u1', name: 'Pessoa Teste', role }, signOut });
}

beforeEach(() => jest.clearAllMocks());

describe('MainMenu', () => {
  it('mostra o nome e os itens de quem é USER, sem Moderação', async () => {
    signedInAs('USER');
    await render(<MainMenu onClose={jest.fn()} />);

    expect(screen.getByText('Pessoa Teste')).toBeOnTheScreen();
    for (const item of ['Início', 'Meu perfil', 'Minhas avaliações', 'Sair da conta']) {
      expect(screen.getByRole('button', { name: item })).toBeOnTheScreen();
    }
    expect(screen.queryByRole('button', { name: 'Moderação' })).toBeNull();
  });

  it('ADMIN também vê Moderação', async () => {
    signedInAs('ADMIN');
    await render(<MainMenu onClose={jest.fn()} />);
    expect(screen.getByRole('button', { name: 'Moderação' })).toBeOnTheScreen();
  });

  it('um item fecha o menu e navega', async () => {
    signedInAs('USER');
    const onClose = jest.fn();
    await render(<MainMenu onClose={onClose} />);

    await fireEvent.press(screen.getByRole('button', { name: 'Minhas avaliações' }));

    expect(onClose).toHaveBeenCalled();
    expect(router.push).toHaveBeenCalledWith('/minhas-avaliacoes');
  });

  it('sair só encerra a sessão depois de confirmar', async () => {
    signedInAs('USER');
    await render(<MainMenu onClose={jest.fn()} />);

    mockedConfirm.mockResolvedValueOnce(false);
    await fireEvent.press(screen.getByRole('button', { name: 'Sair da conta' }));
    expect(signOut).not.toHaveBeenCalled();

    mockedConfirm.mockResolvedValueOnce(true);
    await fireEvent.press(screen.getByRole('button', { name: 'Sair da conta' }));
    expect(signOut).toHaveBeenCalledTimes(1);
  });
});
