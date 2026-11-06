import { defineConfig, devices } from '@playwright/test';

// Precisa do backend (8080) e do app web (8081) no ar: ver "Testes e2e" no README.
export default defineConfig({
  testDir: './e2e',
  outputDir: './e2e-results',
  // Um por vez: os testes dividem o banco de desenvolvimento e a fila de moderação
  workers: 1,
  fullyParallel: false,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: 'list',
  use: {
    ...devices['Pixel 7'],
    baseURL: 'http://localhost:8081',
    locale: 'pt-BR',
    screenshot: 'only-on-failure',
  },
});
