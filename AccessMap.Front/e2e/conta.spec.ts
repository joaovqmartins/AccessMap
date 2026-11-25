import { expect, test } from '@playwright/test';

import { PASSWORD, acceptNextDialog, fillLogin, newAccount, print, signIn } from './support';

// Fluxos de conta que todo usuário percorre (#137): cadastro, login, sair e troca de senha.

function phoneForUi() {
  return `119${String(Date.now()).slice(-8)}`;
}

test('cadastro pela tela leva para a Home já logado', async ({ page }) => {
  await page.goto('/cadastro');
  await page.getByLabel('Nome').fill('Pessoa E2E');
  await page.getByLabel('Telefone').fill(phoneForUi());
  await page.getByLabel('E-mail').fill(`e2e-${Date.now()}@teste.com`);
  await page.getByLabel('Senha').fill(PASSWORD);
  await page.getByRole('button', { name: 'Cadastrar' }).click();

  await expect(page).toHaveURL('/');
  await expect(page.getByRole('button', { name: 'Abrir menu' })).toBeVisible();
  await print(page, 'conta-cadastro-home');
});

test('cadastro valida os campos antes de enviar', async ({ page }) => {
  await page.goto('/cadastro');
  await page.getByRole('button', { name: 'Cadastrar' }).click();

  await expect(page.getByText('Informe seu nome')).toBeVisible();
  await expect(page.getByText('Informe um telefone com DDD (10 ou 11 dígitos)')).toBeVisible();
  await expect(page.getByText('Informe um e-mail válido')).toBeVisible();
  await expect(page.getByText('A senha deve ter no mínimo 8 caracteres')).toBeVisible();
});

test('esqueci minha senha pede o código pelo e-mail (#109)', async ({ page }) => {
  await page.goto('/login');
  await page.getByRole('link', { name: 'Esqueci minha senha' }).click();

  await expect(page).toHaveURL('/esqueci-senha');
  await page.getByLabel('E-mail').fill(`ninguem-${Date.now()}@teste.com`);
  await page.getByRole('button', { name: 'Enviar código' }).click();

  // A resposta é a mesma exista ou não a conta
  await expect(page.getByText('Se houver uma conta com esse e-mail, enviamos um código.', { exact: false })).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Código' })).toBeVisible();
  await print(page, 'conta-esqueci-senha');
});

test('senha errada mostra a mensagem certa', async ({ page }) => {
  const account = await newAccount();

  await page.goto('/login');
  await fillLogin(page, account.phone, 'senha-errada-123');

  await expect(page.getByText('Telefone ou senha incorretos')).toBeVisible();
  await expect(page).toHaveURL('/login');
});

test('sair pelo menu e entrar de novo', async ({ page }) => {
  const account = await newAccount();
  await signIn(page, account);

  await page.getByRole('button', { name: 'Abrir menu' }).click();
  const dialog = acceptNextDialog(page);
  await page.getByRole('button', { name: 'Sair da conta' }).click();
  expect(await dialog).toContain('Sair da conta');
  await expect(page).toHaveURL('/login');

  await signIn(page, account);
});

test('troca de senha encerra a sessão e exige a senha nova', async ({ page }) => {
  const account = await newAccount();
  const newPassword = 'e2e-senha-nova-123';
  await signIn(page, account);

  await page.goto('/perfil');
  await page.getByRole('button', { name: 'Alterar senha' }).click();
  await page.getByLabel('Senha atual', { exact: true }).fill(PASSWORD);
  await page.getByLabel('Nova senha', { exact: true }).fill(newPassword);
  await page.getByLabel('Confirme a nova senha', { exact: true }).fill(newPassword);
  const notice = acceptNextDialog(page);
  await page.getByRole('button', { name: 'Alterar senha' }).click();

  expect(await notice).toContain('todas as sessões foram encerradas');
  await expect(page).toHaveURL('/login');

  await fillLogin(page, account.phone, PASSWORD);
  await expect(page.getByText('Telefone ou senha incorretos')).toBeVisible();

  await fillLogin(page, account.phone, newPassword);
  await expect(page).toHaveURL('/');
});
