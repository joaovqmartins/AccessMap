import { expect, test } from '@playwright/test';

import {
  COMMENT_PREFIX,
  PASSWORD,
  acceptNextDialog,
  clearLeftovers,
  fillLogin,
  newAccount,
  print,
  publish,
  signIn,
  unique,
} from './support';

// Ciclo da avaliação (#137): avaliar, conferir média e barras, editar, excluir, permissões e validação.

test.beforeAll(clearLeftovers);

const EMPTY_PLACE = 'Este local ainda não tem avaliações de acessibilidade. Seja a primeira pessoa a avaliar!';

test('avaliar, editar e excluir atualizam o local e a lista', async ({ page }) => {
  const placeId = unique('e2e-ciclo');
  await signIn(page, await newAccount());

  await page.goto(`/locais/${placeId}?name=Local%20E2E`);
  await expect(page.getByText(EMPTY_PLACE)).toBeVisible();

  // Avaliar: nota + tags + comentário
  await page.getByRole('button', { name: 'Avaliar este local' }).click();
  await page.getByRole('radio', { name: '4 estrelas' }).click();
  await page.getByRole('radio', { name: 'Rampas e entradas: Adequado' }).click();
  await page.getByRole('radio', { name: 'Elevadores: Inadequado' }).click();
  await page.getByLabel('Comentário (opcional)').fill(`${COMMENT_PREFIX} ciclo completo`);
  await page.getByRole('button', { name: 'Publicar avaliação' }).click();

  // De volta ao local: média e barras por característica
  await expect(page).toHaveURL(new RegExp(`/locais/${placeId}`));
  await expect(page.getByText('4,0')).toBeVisible();
  await expect(page.getByText('1 avaliação', { exact: true })).toBeVisible();
  await expect(page.getByText('1 adequado').first()).toBeVisible();
  await expect(page.getByText('1 inadequado').first()).toBeVisible();
  await print(page, 'avaliacao-publicada');

  // Editar a nota
  await page.getByRole('button', { name: 'Ver minha avaliação' }).click();
  await page.getByRole('button', { name: 'Editar avaliação' }).click();
  await page.getByRole('radio', { name: '2 estrelas' }).click();
  await page.getByRole('button', { name: 'Salvar alterações' }).click();
  await expect(page).toHaveURL(/\/avaliacoes\/[^/]+$/);

  // A lista do local mostra o estado novo (regressão do #97)
  await page.goto(`/locais/${placeId}`);
  await expect(page.getByText('2,0')).toBeVisible();

  // Excluir com confirmação: o local volta a "sem avaliações"
  await page.getByRole('button', { name: 'Ver minha avaliação' }).click();
  const dialog = acceptNextDialog(page);
  await page.getByRole('button', { name: 'Excluir avaliação' }).click();
  expect(await dialog).toContain('Tem certeza');
  await expect(page).toHaveURL(new RegExp(`/locais/${placeId}`));
  await expect(page.getByText(EMPTY_PLACE)).toBeVisible();

  await page.goto(`/locais/${placeId}/avaliacoes`);
  await expect(page.getByText('Nenhuma avaliação ainda.')).toBeVisible();
});

test('excluir sem histórico para voltar leva ao local', async ({ page }) => {
  const placeId = unique('e2e-sem-historico');
  const author = await newAccount();
  const review = await publish(author, placeId, 3);
  await signIn(page, author);

  // Abre a avaliação direto pelo endereço: não há tela anterior no histórico do app
  await page.goto(`/avaliacoes/${review.id}`);
  const dialog = acceptNextDialog(page);
  await page.getByRole('button', { name: 'Excluir avaliação' }).click();
  await dialog;

  await expect(page).toHaveURL(new RegExp(`/locais/${placeId}`));
  await expect(page.getByText(EMPTY_PLACE)).toBeVisible();
});

test('outro usuário não vê Editar nem Excluir na avaliação alheia', async ({ page }) => {
  const review = await publish(await newAccount(), unique('e2e-alheia'), 4);
  await signIn(page, await newAccount());

  await page.goto(`/avaliacoes/${review.id}`);

  await expect(page.getByText(review.comment)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Denunciar avaliação' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Editar avaliação' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Excluir avaliação' })).toHaveCount(0);
});

test('sem login, "Entrar para avaliar" volta ao formulário do local depois do login (#102)', async ({ page }) => {
  const placeId = unique('e2e-redirect');
  const account = await newAccount();

  await page.goto(`/locais/${placeId}?name=Local%20E2E`);
  await page.getByRole('button', { name: 'Entrar para avaliar' }).click();
  await expect(page).toHaveURL(/\/login\?redirect=/);
  await fillLogin(page, account.phone);

  await expect(page).toHaveURL(new RegExp(`/locais/${placeId}/avaliar`));
  await expect(page.getByText('Nota geral')).toBeVisible();
});

test('sem conta, o cadastro também volta ao formulário do local (#102)', async ({ page }) => {
  const placeId = unique('e2e-redirect-cadastro');

  await page.goto(`/locais/${placeId}?name=Local%20E2E`);
  await page.getByRole('button', { name: 'Entrar para avaliar' }).click();
  await page.getByRole('link', { name: 'Não tem conta? Cadastre-se' }).click();
  await expect(page).toHaveURL(/\/cadastro\?redirect=/);

  // getByRole: a tela de login continua montada (escondida) embaixo do cadastro, com os mesmos rótulos
  await page.getByRole('textbox', { name: 'Nome' }).fill('Pessoa E2E');
  await page.getByRole('textbox', { name: 'Telefone' }).fill(`119${String(Date.now()).slice(-8)}`);
  await page.getByRole('textbox', { name: 'Senha', exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Cadastrar' }).click();

  await expect(page).toHaveURL(new RegExp(`/locais/${placeId}/avaliar`));
});

test('redirect para fora do app é ignorado e o login vai para a Home', async ({ page }) => {
  const account = await newAccount();

  await page.goto('/login?redirect=//exemplo.invalid/roubo');
  await fillLogin(page, account.phone);

  await expect(page).toHaveURL('/');
});

test('formulário mostra erro de nota e de característica, e corta o comentário no limite', async ({ page }) => {
  await signIn(page, await newAccount());
  await page.goto(`/locais/${unique('e2e-validacao')}/avaliar`);

  await page.getByRole('button', { name: 'Publicar avaliação' }).click();
  await expect(page.getByText('Escolha uma nota de 1 a 5')).toBeVisible();
  await expect(page.getByText('Avalie ao menos uma característica de acessibilidade')).toBeVisible();

  // O campo não aceita passar de 255 caracteres, então não há como chegar no erro genérico do servidor
  const comment = page.getByLabel('Comentário (opcional)');
  await comment.fill(`${COMMENT_PREFIX} ${'x'.repeat(300)}`);
  expect((await comment.inputValue()).length).toBe(255);
});

test('quem já avaliou o local vai direto para a própria avaliação', async ({ page }) => {
  const placeId = unique('e2e-repetida');
  const author = await newAccount();
  await publish(author, placeId, 5);
  await signIn(page, author);

  await page.goto(`/locais/${placeId}/avaliar`);

  await expect(page).toHaveURL(/\/avaliacoes\/[^/]+\?jaAvaliado=1/);
  await expect(page.getByText('Você já avaliou este local. Para mudar algo, edite a sua avaliação.')).toBeVisible();
});
