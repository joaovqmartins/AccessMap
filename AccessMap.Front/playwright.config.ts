import path from 'node:path';

import { defineConfig, devices } from '@playwright/test';

import { E2E_API_URL, E2E_APP_URL } from './e2e/support';

// Ambiente isolado do e2e (#128): banco próprio (postgres-e2e, porta 5433), API na 8090 e app web na 8091,
// para rodar ao lado do ambiente de desenvolvimento (8080/8081) sem misturar dados.
// O banco precisa estar no ar: "npm run e2e:ci" sobe, roda e derruba tudo. Ver "Testes e2e" no README.
const isWindows = process.platform === 'win32';
const inCi = !!process.env.CI;

export default defineConfig({
  testDir: './e2e',
  outputDir: './e2e-results',
  // Um por vez: os testes dividem a fila de moderação do ADMIN
  workers: 1,
  fullyParallel: false,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: inCi ? [['list'], ['html', { outputFolder: 'e2e-results/relatorio', open: 'never' }]] : 'list',
  use: {
    ...devices['Pixel 7'],
    baseURL: E2E_APP_URL,
    locale: 'pt-BR',
    screenshot: 'only-on-failure',
  },
  webServer: [
    {
      // API com o perfil e2e (application-e2e.yaml)
      // Caminho explícito: no Windows o cmd não acha o mvnw.cmd só pelo nome
      command: `${isWindows ? '.\\mvnw.cmd' : './mvnw'} -q spring-boot:run -Dspring-boot.run.profiles=e2e`,
      cwd: path.join(__dirname, '..', 'AccessMap.Back'),
      url: `${E2E_API_URL}/v3/api-docs`,
      // Primeira execução baixa as dependências do Maven
      timeout: 240_000,
      reuseExistingServer: !inCi,
      stdout: 'ignore',
      stderr: 'pipe',
    },
    {
      // App web apontando para a API do e2e
      command: `npx expo start --web --port ${new URL(E2E_APP_URL).port}`,
      url: E2E_APP_URL,
      timeout: 180_000,
      reuseExistingServer: !inCi,
      env: { EXPO_PUBLIC_API_URL: E2E_API_URL, CI: '1' },
      stdout: 'ignore',
      stderr: 'pipe',
    },
  ],
});
