import { expect, test } from '@playwright/test';

import { clearLeftovers, newAccount, print, publish, signIn, unique } from './support';

// Navegação da Home (#137 + achados do teste de fluxo): menu ☰, barra inferior, Minhas avaliações
// e a tela de erro (#133).

test.beforeAll(clearLeftovers);

test('menu ☰ abre Minhas avaliações com o nome de cada local', async ({ page }) => {
  const author = await newAccount();
  await publish(author, unique('e2e-menu'), 4, undefined, 'Padaria E2E');
  await signIn(page, author);

  await page.getByRole('button', { name: 'Abrir menu' }).click();
  // "Meu perfil" também é o rótulo do avatar da Home: confere o item que só existe no menu
  await expect(page.getByRole('button', { name: 'Sair da conta' })).toBeVisible();
  // Conta comum não vê a moderação
  await expect(page.getByRole('button', { name: 'Moderação' })).toHaveCount(0);
  await print(page, 'navegacao-menu');

  await page.getByRole('button', { name: 'Minhas avaliações' }).click();
  await expect(page).toHaveURL('/minhas-avaliacoes');
  await expect(page.getByText('Padaria E2E')).toBeVisible();
  await print(page, 'navegacao-minhas-avaliacoes');
});

test('barra inferior: Restaurantes seleciona a categoria e Review abre Minhas avaliações', async ({ page }) => {
  await signIn(page, await newAccount());

  await page.getByRole('tab', { name: 'Restaurantes' }).click();
  await expect(page.getByRole('tab', { name: 'Restaurantes' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByRole('button', { name: 'Categoria Restaurantes' })).toHaveAttribute('aria-pressed', 'true');

  await page.getByRole('tab', { name: 'Home' }).click();
  await expect(page.getByRole('button', { name: 'Categoria Mais próximos' })).toHaveAttribute('aria-pressed', 'true');

  await page.getByRole('tab', { name: 'Minhas avaliações' }).click();
  await expect(page).toHaveURL('/minhas-avaliacoes');
  await expect(page.getByText('Você ainda não avaliou nenhum local.')).toBeVisible();
});

test('tela de erro: "Tentar de novo" re-renderiza e "Voltar ao início" sai dela (#133)', async ({ page }) => {
  await page.goto('/dev/erro');
  await expect(page.getByRole('alert').getByText('Algo deu errado')).toBeVisible();
  await print(page, 'navegacao-tela-de-erro');

  // A rota de teste para de quebrar quando esta chave existe
  await page.evaluate(() => sessionStorage.setItem('accessmap.devErroConsertado', '1'));
  await page.getByRole('button', { name: 'Tentar de novo' }).click();
  await expect(page.getByText('Tela recuperada')).toBeVisible();

  await page.evaluate(() => sessionStorage.removeItem('accessmap.devErroConsertado'));
  await page.goto('/dev/erro');
  await page.getByRole('button', { name: 'Voltar ao início' }).click();
  await expect(page).toHaveURL(/\/(login)?$/);
});
