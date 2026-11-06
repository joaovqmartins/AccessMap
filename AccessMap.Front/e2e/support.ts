import { expect, type Page } from '@playwright/test';
import { Client } from 'pg';

const API = process.env.E2E_API_URL ?? 'http://localhost:8080';
// Credenciais do docker-compose de desenvolvimento (README)
const DATABASE = process.env.E2E_DATABASE_URL ?? 'postgres://postgres:accessmap123@localhost:5432/accessmap';

export const PASSWORD = 'e2e-senha-local';
/** Toda avaliação criada aqui começa assim, para limpar o que sobrar de uma execução anterior. */
export const COMMENT_PREFIX = 'e2e:';

export type Account = { id: string; phone: string; token: string };

let sequence = 0;

export function unique(prefix: string) {
  sequence += 1;
  return `${prefix}-${Date.now()}-${sequence}`;
}

function uniquePhone() {
  sequence += 1;
  return `119${String(Date.now() + sequence).slice(-8)}`;
}

async function sql(text: string, values: unknown[] = []) {
  const client = new Client({ connectionString: DATABASE });
  await client.connect();
  try {
    await client.query(text, values);
  } finally {
    await client.end();
  }
}

async function post<T>(path: string, body: unknown, token?: string): Promise<T> {
  const response = await fetch(API + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(`${path}: ${response.status} ${await response.text()}`);
  return response.json() as Promise<T>;
}

export async function newAccount(): Promise<Account> {
  const phone = uniquePhone();
  const created = await post<{ accessToken: string; user: { id: string } }>('/api/auth/register', {
    name: 'Pessoa E2E',
    phone,
    password: PASSWORD,
  });
  return { id: created.user.id, phone, token: created.accessToken };
}

/** Conta com 40 dias: já passou do prazo de conta nova, então as denúncias dela contam. */
export async function establishedAccount() {
  const account = await newAccount();
  await sql(`update users set created_at = now() - interval '40 days' where id = $1`, [account.id]);
  return account;
}

/** Conta antiga com 5 avaliações publicadas: a próxima sai como Confiável. */
export async function trustedAccount() {
  const account = await establishedAccount();
  for (let i = 0; i < 5; i += 1) {
    await publish(account, unique('e2e-historico'), 4);
  }
  // Fora da última hora, para não esbarrar no limite de avaliações
  await sql(`update reviews set created_at = now() - interval '2 days' where user_id = $1`, [account.id]);
  return account;
}

export async function adminAccount() {
  const account = await establishedAccount();
  await sql(`update users set role = 'ADMIN' where id = $1`, [account.id]);
  return account;
}

export async function publish(author: Account, placeId: string, rating: number, comment = unique(COMMENT_PREFIX)) {
  const review = await post<{ id: string }>(
    '/api/reviews',
    { placeId, rating, comment, tags: { RAMPAS_E_ENTRADAS: 'ADEQUADO' } },
    author.token,
  );
  return { id: review.id, comment };
}

export async function clearLeftovers() {
  await sql(`update reviews set suspicion_reason = null where comment like $1`, [`${COMMENT_PREFIX}%`]);
  await sql(`update reviews set status = 'REMOVIDA' where comment like $1 and status = 'OCULTA'`, [`${COMMENT_PREFIX}%`]);
}

export async function signIn(page: Page, account: Account) {
  await page.goto('/login');
  await page.getByLabel('Telefone').fill(account.phone);
  await page.getByLabel('Senha').fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page).toHaveURL('/');
}

/** Confirma a próxima caixa do navegador e devolve o texto dela. */
export function acceptNextDialog(page: Page) {
  return new Promise<string>((resolve) => {
    page.once('dialog', async (dialog) => {
      const message = dialog.message();
      await dialog.accept();
      resolve(message);
    });
  });
}

export function print(page: Page, name: string) {
  return page.screenshot({ path: `e2e-results/prints/${name}.png`, fullPage: true });
}
