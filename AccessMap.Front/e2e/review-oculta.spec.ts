import { expect, test } from '@playwright/test';

import { acceptNextDialog, establishedAccount, hideByReports, print, publish, signIn, unique } from './support';

const NOTICE = 'Esta avaliação está em análise e não aparece para outras pessoas.';

test('o autor vê a review oculta em análise, sem editar, e pode excluir', async ({ page }) => {
  const author = await establishedAccount();
  const placeId = unique('e2e-oculta-autor');
  const review = await publish(author, placeId, 5);
  await hideByReports(review.id);

  await page.goto(`/avaliacoes/${review.id}`);
  await expect(page.getByText('Esta avaliação não existe mais.')).toBeVisible();

  await signIn(page, author);
  await page.goto(`/locais/${placeId}`);
  await expect(page.getByRole('button', { name: 'Ver minha avaliação' })).toBeVisible();

  await page.goto(`/avaliacoes/${review.id}`);
  await expect(page.getByText(NOTICE)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Editar avaliação' })).toHaveCount(0);
  await print(page, 'review-em-analise');

  await page.goto(`/avaliacoes/${review.id}/editar`);
  await expect(page.getByText('Esta avaliação está em análise e não pode ser editada.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Salvar alterações' })).toHaveCount(0);

  await page.goto(`/avaliacoes/${review.id}`);
  const dialog = acceptNextDialog(page);
  await page.getByRole('button', { name: 'Excluir avaliação' }).click();
  expect(await dialog).toContain('conta como remoção pela moderação');

  await page.goto(`/locais/${placeId}`);
  await expect(page.getByRole('button', { name: 'Avaliar este local' })).toBeVisible();
});
