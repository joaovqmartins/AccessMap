import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';

import { newAccount, publish, signIn, unique } from './support';

// Auditoria automática de acessibilidade (#56): roda o axe-core nas telas principais da versão web
// e falha em violações graves (serious/critical) das regras WCAG 2.1 A e AA.
// Não substitui o teste com leitor de tela no aparelho (roteiro em docs/acessibilidade/auditoria.md).

const WCAG_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

async function audit(page: Page, screen: string) {
  // Espera a tela assentar (fontes, listas e chamadas à API) antes de analisar
  await page.waitForLoadState('networkidle');
  const { violations } = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze();
  const serious = violations
    .filter((v) => v.impact === 'serious' || v.impact === 'critical')
    .map((v) => `${v.id} (${v.impact}): ${v.help} — ${v.nodes.map((n) => n.target.join(' ')).join(' | ')}`);
  expect(serious, `violações graves de acessibilidade em ${screen}`).toEqual([]);
}

test('telas de quem não entrou: login e cadastro', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('button', { name: 'Entrar', exact: true })).toBeVisible();
  await audit(page, 'login');

  await page.goto('/cadastro');
  await expect(page.getByRole('button', { name: 'Cadastrar' })).toBeVisible();
  await audit(page, 'cadastro');
});

test('telas de quem entrou: início, menu, perfil, local, formulário e minhas avaliações', async ({ page }) => {
  const account = await newAccount();
  const placeId = unique('e2e-a11y');
  await publish(account, placeId, 4, undefined, 'Local A11y');
  await signIn(page, account);

  await audit(page, 'início');

  await page.getByRole('button', { name: 'Abrir menu' }).click();
  await expect(page.getByRole('button', { name: 'Sair da conta' })).toBeVisible();
  // O menu abre com fade: no meio da animação o texto ainda está semitransparente e o axe mede contraste baixo
  await page.waitForTimeout(600);
  await audit(page, 'menu');
  await page.getByRole('button', { name: 'Fechar menu' }).first().click();

  await page.getByRole('button', { name: 'Filtros de acessibilidade' }).click();
  await expect(page.getByRole('button', { name: 'Aplicar', exact: true })).toBeVisible();
  await page.waitForTimeout(600); // painel abre com animação
  await audit(page, 'filtros');
  await page.getByRole('button', { name: 'Fechar sem aplicar' }).click();

  await page.goto('/perfil');
  await expect(page.getByRole('button', { name: 'Editar perfil' })).toBeVisible();
  await audit(page, 'perfil');

  await page.goto(`/locais/${placeId}?name=Local%20A11y`);
  await expect(page.getByRole('button', { name: 'Ver minha avaliação' })).toBeVisible();
  await audit(page, 'detalhes do local');

  await page.goto(`/locais/${unique('e2e-a11y-novo')}/avaliar`);
  await expect(page.getByRole('button', { name: 'Publicar avaliação' })).toBeVisible();
  await audit(page, 'formulário de avaliação');

  await page.goto('/minhas-avaliacoes');
  await expect(page.getByText('Local A11y')).toBeVisible();
  await audit(page, 'minhas avaliações');
});
