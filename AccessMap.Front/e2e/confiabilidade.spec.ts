import { expect, test, type Browser, type Page } from '@playwright/test';

import {
  acceptNextDialog,
  adminAccount,
  clearLeftovers,
  establishedAccount,
  newAccount,
  print,
  publish,
  signIn,
  trustedAccount,
  unique,
  type Account,
} from './support';

test.beforeAll(clearLeftovers);

function moderationCard(page: Page, comment: string, button: string) {
  return page
    .locator('div', { hasText: comment })
    .filter({ has: page.getByRole('button', { name: button, exact: true }) })
    .last();
}

async function openQueue(page: Page) {
  await page.goto('/perfil');
  await page.getByRole('button', { name: 'Moderação', exact: true }).click();
  await expect(page).toHaveURL('/moderacao');
}

async function reportOnScreen(browser: Browser, reviewId: string, reporter: Account) {
  const context = await browser.newContext({ baseURL: 'http://localhost:8081' });
  const page = await context.newPage();
  await signIn(page, reporter);
  await page.goto(`/avaliacoes/${reviewId}`);
  await page.getByRole('button', { name: 'Denunciar avaliação' }).click();
  await page.getByRole('radio', { name: 'Propaganda ou spam' }).click();
  const dialog = acceptNextDialog(page);
  await page.getByRole('button', { name: 'Enviar denúncia' }).click();
  expect(await dialog).toContain('Tem certeza');
  await expect(page.getByText('Denúncia enviada. Obrigado por avisar.')).toBeVisible();
  await context.close();
}

/** Review com 3 denúncias de contas antigas, num local onde sobra uma review de nota 1. */
async function hiddenReview(browser: Browser) {
  const placeId = unique('e2e-oculta');
  const review = await publish(await establishedAccount(), placeId, 5);
  await publish(await establishedAccount(), placeId, 1);
  for (let i = 0; i < 3; i += 1) {
    await reportOnScreen(browser, review.id, await establishedAccount());
  }
  return { ...review, placeId };
}

/** Local com 5 reviews de nota 5, e uma conta nova dando nota 1. */
async function outlierReview() {
  const placeId = unique('e2e-destoante');
  for (let i = 0; i < 5; i += 1) {
    await publish(await establishedAccount(), placeId, 5);
  }
  return { ...(await publish(await newAccount(), placeId, 1)), placeId };
}

test('conta nova publica pela tela e o card mostra o selo', async ({ page }) => {
  const placeId = unique('e2e-selo');
  await signIn(page, await newAccount());

  await page.goto(`/locais/${placeId}?name=Local%20E2E`);
  await page.getByRole('button', { name: 'Avaliar este local' }).click();
  await page.getByRole('radio', { name: '4 estrelas' }).click();
  await page.getByRole('radio', { name: 'Rampas e entradas: Adequado' }).click();
  await page.getByRole('button', { name: 'Publicar avaliação' }).click();

  await expect(page.getByText('Avaliado por conta nova')).toBeVisible();
  await print(page, 'selo-conta-nova');
});

test('conta confiável aparece com o selo para quem não está logado', async ({ page }) => {
  const review = await publish(await trustedAccount(), unique('e2e-selo'), 5);

  await page.goto(`/avaliacoes/${review.id}`);

  await expect(page.getByText('✓ Avaliador confiável')).toBeVisible();
  await print(page, 'selo-confiavel');
});

test('conta regular não tem selo', async ({ page }) => {
  const review = await publish(await establishedAccount(), unique('e2e-selo'), 5);

  await page.goto(`/avaliacoes/${review.id}`);

  await expect(page.getByText(review.comment)).toBeVisible();
  await expect(page.getByText('Avaliador confiável')).toHaveCount(0);
  await expect(page.getByText('Avaliado por conta nova')).toHaveCount(0);
});

test('três denúncias tiram a review do ar e o ADMIN restaura', async ({ page, browser }) => {
  const review = await hiddenReview(browser);

  await page.goto(`/avaliacoes/${review.id}`);
  await expect(page.getByText('Esta avaliação não existe mais.')).toBeVisible();
  await page.goto(`/locais/${review.placeId}`);
  await expect(page.getByText('1,0', { exact: true })).toBeVisible();

  await signIn(page, await adminAccount());
  await openQueue(page);
  const card = moderationCard(page, review.comment, 'Restaurar');
  await expect(card.getByText('Oculta · Denúncias (3)')).toBeVisible();
  await print(page, 'fila-com-review-oculta');

  const dialog = acceptNextDialog(page);
  await card.getByRole('button', { name: 'Restaurar' }).click();
  expect(await dialog).toContain('Restaurar avaliação');
  await expect(page.getByText(review.comment)).toHaveCount(0);

  await page.goto(`/locais/${review.placeId}`);
  await expect(page.getByText('3,0', { exact: true })).toBeVisible();
});

test('nota destoante continua no ar, entra na fila e o ADMIN mantém', async ({ page }) => {
  const review = await outlierReview();

  await page.goto(`/avaliacoes/${review.id}`);
  await expect(page.getByText(review.comment)).toBeVisible();

  await signIn(page, await adminAccount());
  await openQueue(page);
  const card = moderationCard(page, review.comment, 'Manter');
  await expect(card.getByText('Publicada · Nota destoante')).toBeVisible();
  await print(page, 'fila-com-nota-destoante');

  const dialog = acceptNextDialog(page);
  await card.getByRole('button', { name: 'Manter' }).click();
  await dialog;
  await expect(page.getByText(review.comment)).toHaveCount(0);

  await page.goto(`/avaliacoes/${review.id}`);
  await expect(page.getByText(review.comment)).toBeVisible();
});

test('o ADMIN remove e a review some para todos', async ({ page }) => {
  const review = await outlierReview();

  await signIn(page, await adminAccount());
  await openQueue(page);
  const dialog = acceptNextDialog(page);
  await moderationCard(page, review.comment, 'Remover').getByRole('button', { name: 'Remover' }).click();
  await dialog;
  await expect(page.getByText(review.comment)).toHaveCount(0);

  await page.goto(`/avaliacoes/${review.id}`);
  await expect(page.getByText('Esta avaliação não existe mais.')).toBeVisible();
});

test('usuário comum não vê nem abre a moderação', async ({ page }) => {
  await signIn(page, await newAccount());

  await page.goto('/perfil');
  await expect(page.getByRole('button', { name: 'Sair da conta' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Moderação', exact: true })).toHaveCount(0);

  await page.goto('/moderacao');
  await expect(page).toHaveURL('/');
});
