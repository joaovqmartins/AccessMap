import { fireEvent, render, screen } from '@testing-library/react-native';
import { router, useLocalSearchParams } from 'expo-router';

import { passwordResetApi } from '../../src/api/auth';
import { ApiError } from '../../src/api/client';
import ForgotPasswordScreen from '../../src/screens/ForgotPasswordScreen';
import { notify } from '../../src/utils/dialog';

jest.mock('expo-router', () => ({
  router: { replace: jest.fn() },
  useLocalSearchParams: jest.fn(),
}));
jest.mock('../../src/api/auth', () => ({
  passwordResetApi: { request: jest.fn(), confirm: jest.fn() },
}));
jest.mock('../../src/utils/dialog', () => ({ notify: jest.fn().mockResolvedValue(undefined) }));

const api = passwordResetApi as jest.Mocked<typeof passwordResetApi>;
const ENVIADO = 'Se houver uma conta com esse e-mail, enviamos um código.';

beforeEach(() => {
  jest.clearAllMocks();
  (useLocalSearchParams as jest.Mock).mockReturnValue({});
  api.request.mockResolvedValue({ message: ENVIADO });
});

async function pedirCodigo(email = 'maria@teste.com') {
  await render(<ForgotPasswordScreen />);
  await fireEvent.changeText(screen.getByLabelText('E-mail'), email);
  await fireEvent.press(screen.getByRole('button', { name: 'Enviar código' }));
  await screen.findByLabelText('Código');
}

async function redefinir(codigo: string, senha: string, confirmacao = senha) {
  await fireEvent.changeText(screen.getByLabelText('Código'), codigo);
  await fireEvent.changeText(screen.getByLabelText('Nova senha'), senha);
  await fireEvent.changeText(screen.getByLabelText('Confirme a nova senha'), confirmacao);
  await fireEvent.press(screen.getByRole('button', { name: 'Redefinir senha' }));
}

describe('ForgotPasswordScreen (#109)', () => {
  it('não pede o código sem um e-mail válido', async () => {
    await render(<ForgotPasswordScreen />);
    await fireEvent.changeText(screen.getByLabelText('E-mail'), 'sem-arroba');
    await fireEvent.press(screen.getByRole('button', { name: 'Enviar código' }));

    expect(screen.getByText('Informe o e-mail da sua conta')).toBeOnTheScreen();
    expect(api.request).not.toHaveBeenCalled();
  });

  it('pede o código e mostra o aviso, que é o mesmo exista ou não a conta', async () => {
    await pedirCodigo(' maria@teste.com ');

    expect(api.request).toHaveBeenCalledWith('maria@teste.com');
    expect(screen.getByText(ENVIADO, { exact: false })).toBeOnTheScreen();
  });

  it('valida código, senha e confirmação antes de enviar', async () => {
    await pedirCodigo();
    await redefinir('12', 'curta', 'outra');

    expect(screen.getByText('O código tem 6 números')).toBeOnTheScreen();
    expect(screen.getByText('A senha deve ter no mínimo 8 caracteres')).toBeOnTheScreen();
    expect(screen.getByText('As senhas não conferem')).toBeOnTheScreen();
    expect(api.confirm).not.toHaveBeenCalled();
  });

  it('com o código certo, troca a senha e volta para o login', async () => {
    api.confirm.mockResolvedValue({ message: 'Senha alterada' });
    await pedirCodigo();
    await redefinir('123456', 'novaSenha123');

    expect(api.confirm).toHaveBeenCalledWith({ email: 'maria@teste.com', code: '123456', password: 'novaSenha123' });
    expect(notify).toHaveBeenCalled();
    expect(router.replace).toHaveBeenCalledWith('/login');
  });

  it('código recusado mostra a mensagem do backend', async () => {
    api.confirm.mockRejectedValue(new ApiError(400, 'Código inválido ou expirado'));
    await pedirCodigo();
    await redefinir('654321', 'novaSenha123');

    expect(await screen.findByText('Código inválido ou expirado')).toBeOnTheScreen();
    expect(router.replace).not.toHaveBeenCalled();
  });
});
