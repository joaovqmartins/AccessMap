// Roda o e2e do começo ao fim no ambiente isolado (#128):
//   1. sobe o banco postgres-e2e (vazio, dados em memória) e espera ficar pronto;
//   2. roda o Playwright, que sobe a API (perfil e2e) e o app web pelo webServer;
//   3. derruba o banco, mesmo se algum teste falhar.
// Repassa os argumentos para o Playwright: npm run e2e:ci -- e2e/conta.spec.ts
// Em Node e não em "&&"/";" no package.json para funcionar igual no Windows e no Linux.
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../..', import.meta.url));
const compose = ['compose', '-f', 'docker-compose.yml', '--profile', 'e2e'];

function run(command, args, cwd) {
  const result = spawnSync(command, args, { cwd, stdio: 'inherit', shell: process.platform === 'win32' });
  return result.status ?? 1;
}

if (run('docker', [...compose, 'up', '-d', '--wait', 'postgres-e2e'], root) !== 0) {
  console.error('Não foi possível subir o banco do e2e. O Docker está rodando?');
  process.exit(1);
}

let status = 1;
try {
  status = run('npx', ['playwright', 'test', ...process.argv.slice(2)], fileURLToPath(new URL('..', import.meta.url)));
} finally {
  run('docker', [...compose, 'rm', '--stop', '--force', 'postgres-e2e'], root);
}
process.exit(status);
