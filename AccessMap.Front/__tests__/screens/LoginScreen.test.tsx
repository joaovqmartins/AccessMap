import { fireEvent, render, screen } from '@testing-library/react-native';
import { useLocalSearchParams } from 'expo-router';

import { ApiError } from '../../src/api/client';
import { useAuth } from '../../src/auth/AuthContext';
import LoginScreen from '../../src/screens/LoginScreen';
import { takePendingRedirect } from '../../src/utils/navigation';

jest.mock('expo-router', () => {
  const { Text } = require('react-native');
  return {
    useLocalSearchParams: jest.fn(),
    // Link vira texto: o teste confere só para onde ele aponta
    Link: ({ href, children }: { href: unknown; children: React.ReactNode }) => (
      <Text testID="link" accessibilityHint={JSON.stringify(href)}>{children}</Text>
    ),
  };
});
jest.mock('../../src/auth/AuthContext', () => ({ useAuth: jest.fn() }));

const signIn = jest.fn();
const mockedParams = useLocalSearchParams as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  takePendingRedirect();
  mockedParams.mockReturnValue({});
  (useAuth as jest.Mock).mockReturnValue({ signIn });
});

async function fill(phone: string, password: string) {
  await fireEvent.changeText(screen.getByLabelText('Telefone'), phone);
  await fireEvent.changeText(screen.getByLabelText('Senha'), password);
  await fireEvent.press(screen.getByRole('button', { name: 'Entrar' }));
}

describe('LoginScreen', () => {
  it('valida antes de enviar', async () => {
    await render(<LoginScreen />);
    await fireEvent.press(screen.getByRole('button', { name: 'Entrar' }));

    expect(screen.getByText('Informe um telefone com DDD (10 ou 11 dígitos)')).toBeOnTheScreen();
    expect(screen.getByText('Informe sua senha')).toBeOnTheScreen();
    expect(signIn).not.toHaveBeenCalled();
  });

  it('manda o telefone só com dígitos, mesmo digitado com máscara', async () => {
    signIn.mockResolvedValue(undefined);
    await render(<LoginScreen />);

    await fill('(11) 91234-5678', 'senha-forte');

    expect(signIn).toHaveBeenCalledWith({ phone: '11912345678', password: 'senha-forte' });
  });

  it('401 vira "Telefone ou senha incorretos"', async () => {
    signIn.mockRejectedValue(new ApiError(401, 'Credenciais inválidas'));
    await render(<LoginScreen />);

    await fill('11912345678', 'errada123');

    expect(await screen.findByText('Telefone ou senha incorretos')).toBeOnTheScreen();
  });

  it('outros erros mostram a mensagem do backend', async () => {
    signIn.mockRejectedValue(new ApiError(429, 'Muitas tentativas. Tente novamente em 15 minutos.'));
    await render(<LoginScreen />);

    await fill('11912345678', 'qualquer1');

    expect(await screen.findByText('Muitas tentativas. Tente novamente em 15 minutos.')).toBeOnTheScreen();
  });

  describe('volta ao local depois do login (#102)', () => {
    it('guarda o redirect interno para o _layout usar quando a sessão abrir', async () => {
      mockedParams.mockReturnValue({ redirect: '/locais/abc/avaliar' });
      signIn.mockResolvedValue(undefined);
      await render(<LoginScreen />);

      await fill('11912345678', 'senha-forte');

      expect(takePendingRedirect()).toBe('/locais/abc/avaliar');
    });

    it('ignora redirect para fora do app', async () => {
      mockedParams.mockReturnValue({ redirect: '//exemplo.com' });
      signIn.mockResolvedValue(undefined);
      await render(<LoginScreen />);

      await fill('11912345678', 'senha-forte');

      expect(takePendingRedirect()).toBeNull();
    });

    it('login recusado descarta o redirect guardado', async () => {
      mockedParams.mockReturnValue({ redirect: '/locais/abc/avaliar' });
      signIn.mockRejectedValue(new ApiError(401, 'x'));
      await render(<LoginScreen />);

      await fill('11912345678', 'errada123');
      await screen.findByText('Telefone ou senha incorretos');

      expect(takePendingRedirect()).toBeNull();
    });

    it('o link de cadastro leva o redirect junto', async () => {
      mockedParams.mockReturnValue({ redirect: '/locais/abc/avaliar' });
      await render(<LoginScreen />);

      const href = JSON.parse(screen.getByTestId('link').props.accessibilityHint);
      expect(href).toEqual({ pathname: '/cadastro', params: { redirect: '/locais/abc/avaliar' } });
    });
  });
});
