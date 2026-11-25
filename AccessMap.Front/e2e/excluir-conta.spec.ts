import { expect, test } from '@playwright/test';

import { acceptNextDialog, establishedAccount, PASSWORD, publish, signIn, unique } from './support';

test('excluir a conta pelo perfil: pede a senha, volta ao login e a avaliação fica anônima', async ({ page }) => {
  const account = await establishedAccount();
  const review = await publish(account, unique('e2e-excluir-conta'), 4);
  await signIn(page, account);

  await page.goto('/perfil');
  await page.getByRole('button', { name: 'Excluir minha conta' }).click();

  await page.getByLabel('Senha atual').fill('senha-errada-123');
  const firstDialog = acceptNextDialog(page);
  await page.getByRole('button', { name: 'Excluir minha conta' }).click();
  await firstDialog;
  await expect(page.getByText('Senha atual incorreta')).toBeVisible();

  await page.getByLabel('Senha atual').fill(PASSWORD);
  const confirmDialog = acceptNextDialog(page);
  const doneDialog = new Promise<string>((resolve) => {
    confirmDialog.then(() => page.once('dialog', async (d) => { resolve(d.message()); await d.accept(); }));
  });
  await page.getByRole('button', { name: 'Excluir minha conta' }).click();
  expect(await confirmDialog).toContain('continuam publicadas');
  expect(await doneDialog).toContain('Conta excluída');
  await expect(page).toHaveURL('/login');

  await page.getByLabel('Telefone').fill(account.phone);
  await page.getByLabel('Senha').fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page.getByText('Telefone ou senha incorretos')).toBeVisible();

  await page.goto(`/avaliacoes/${review.id}`);
  await expect(page.getByText(review.comment)).toBeVisible();
  await expect(page.getByText('Sua avaliação')).toHaveCount(0);
});
