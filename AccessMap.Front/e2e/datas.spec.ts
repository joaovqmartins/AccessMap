import { expect, test } from '@playwright/test';

import { establishedAccount, publish, setCreatedAt, unique } from './support';

// O celular de quem lê está no horário de Brasília (UTC-3)
test.use({ timezoneId: 'America/Sao_Paulo' });

test('avaliação feita às 22:30 em Brasília mostra o mesmo dia, e não o seguinte (#115)', async ({ page }) => {
  const review = await publish(await establishedAccount(), unique('e2e-datas'), 4);
  // 18/10 às 22:30 em Brasília é 19/10 às 01:30 em UTC, que é como o banco guarda
  await setCreatedAt(review.id, '2026-10-19 01:30:00');

  await page.goto(`/avaliacoes/${review.id}`);

  await expect(page.getByText(review.comment)).toBeVisible();
  await expect(page.getByText('18/10/2026', { exact: false })).toBeVisible();
  await expect(page.getByText('19/10/2026', { exact: false })).toHaveCount(0);
});
