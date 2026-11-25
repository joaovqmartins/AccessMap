# ♿ AccessMap

[![Backend](https://github.com/joaovqmartins/AccessMap/actions/workflows/backend.yml/badge.svg)](https://github.com/joaovqmartins/AccessMap/actions/workflows/backend.yml)
[![App](https://github.com/joaovqmartins/AccessMap/actions/workflows/app.yml/badge.svg)](https://github.com/joaovqmartins/AccessMap/actions/workflows/app.yml)
[![E2E](https://github.com/joaovqmartins/AccessMap/actions/workflows/e2e.yml/badge.svg)](https://github.com/joaovqmartins/AccessMap/actions/workflows/e2e.yml)

O AccessMap é um aplicativo mobile que permite à comunidade mapear colaborativamente pontos de acessibilidade e barreiras urbanas. 

O objetivo é fornecer informações reais e atualizadas sobre a acessibilidade dos espaços urbanos, garantindo maior autonomia para pessoas com deficiência ou mobilidade reduzida, além de gerar dados abertos para o embasamento de políticas públicas de inclusão.

## 🚀 Funcionalidades Principais
* **Mapeamento Colaborativo:** Reporte de barreiras (ex: calçadas danificadas, ausência de rampas) e pontos de acessibilidade, com anexo de fotos e localização exata.
* **Confiabilidade das avaliações:** a nota de cada local é uma média ponderada pelo nível de confiança de quem avaliou (conta nova, regular, confiável ou restrita), com limites de frequência, denúncias da comunidade e uma fila de moderação para o ADMIN. As regras, os valores e um exemplo numérico estão em [docs/confiabilidade.md](docs/confiabilidade.md).
* **Filtros Personalizados:** Filtragem de resultados por tipo de necessidade (mobilidade reduzida, deficiência visual, auditiva, etc.).
* **Rotas Acessíveis:** Traçado de rotas entre pontos considerando estritamente caminhos adaptados.

## 🛠️ Tecnologias e Arquitetura

O projeto adota uma arquitetura RESTful com separação clara de responsabilidades:

**Back-end**
* Java 17+ com Spring Boot
* PostgreSQL + PostGIS (via Hibernate Spatial) para dados geoespaciais
* Spring Security + JWT para autenticação
* Swagger/OpenAPI para documentação
* JUnit e Mockito para testes automatizados

**Front-end (Mobile)**
* React Native com Expo (Aplicativo Mobile)
* Jest para testes

**DevOps & CI/CD**
* Git & GitHub Actions
* Deploy contínuo na plataforma Railway

## 🧑‍💻 Ambiente de desenvolvimento

| Parte | Onde roda | Como |
|---|---|---|
| Banco e outras dependências (cache, filas, bancos não relacionais) | Docker | Sobem sozinhas no Run do backend, ou `docker compose up -d` |
| Backend (Spring Boot) | IntelliJ | Run no `BackendApplication` |
| App (Expo) | VS Code / terminal | `npm start` (celular/emulador) ou `npm run web` (navegador) |

**Por que a aplicação não roda no Docker?** Uma *imagem* é uma "foto" pronta do programa e um *container* é essa foto rodando. Se a API rodasse num container, cada mudança no código exigiria gerar a foto de novo (`docker build`). No dia a dia é mais rápido rodar pela IDE, com debugger e recarga. Por isso o [`docker-compose.yml`](docker-compose.yml) tem **só dependências**. O [`AccessMap.Back/Dockerfile`](AccessMap.Back/Dockerfile) continua no repositório, mas é usado só no deploy.

### 1. Variáveis de ambiente (uma vez)

São dois arquivos, cada um com o passo a passo nos comentários. Os dois ficam fora do git.

```bash
cp .env.example .env                                        # backend: preencha o JWT_SECRET
cp AccessMap.Front/.env.example AccessMap.Front/.env.local  # app: preencha a chave do Google (seção abaixo)
```

### 2. Backend

Pré-requisitos: Docker aberto e Java 17+.

1. Abra o projeto no IntelliJ e rode o `BackendApplication` (▶ ao lado do `main`). O diretório de trabalho é a pasta `AccessMap.Back/`, que é o padrão do IntelliJ para o módulo Maven.
2. Ao subir, o Spring Boot:
   - lê o `.env` da raiz sozinho, sem precisar configurar variáveis na IDE;
   - roda `docker compose up` e espera o Postgres ficar pronto (`spring-boot-docker-compose`);
   - aplica as migrações do Flyway.
3. Ao parar a API, o banco continua rodando. Os dados ficam no volume `pgdata` e só somem com `docker compose down -v`.

Pelo terminal funciona igual: `cd AccessMap.Back && ./mvnw spring-boot:run`.

Swagger: http://localhost:8080/swagger-ui.html — use **Authorize** com o `accessToken` de `/api/auth/login`.
Para ver os dados, conecte no banco **`accessmap`** (`localhost:5432`, usuário `postgres`, senha `accessmap123`).

Testes (`./mvnw test`) sobem um Postgres descartável via Testcontainers e precisam do Docker rodando.

### 3. App

Pré-requisitos: Node 20+, backend rodando e, para o celular, o Expo Go.

```bash
cd AccessMap.Front
npm ci
npm start       # QR code para o Expo Go; "a" abre no emulador Android
npm run web     # navegador em http://localhost:8081
```

- O app descobre sozinho o endereço da API (`src/api/baseUrl.ts`): no navegador usa o host da página, e no Expo Go usa o IP do computador que roda o Metro, sempre na porta 8080. O `EXPO_PUBLIC_API_URL` do `.env.local` é opcional e só serve para apontar para outro servidor. O Expo só lê esse arquivo ao iniciar.
- Toda requisição à API desiste em 15 s e mostra "Não foi possível conectar ao servidor".
- No celular e no emulador, o refresh token fica no armazenamento seguro do aparelho (`expo-secure-store`). No navegador ele fica no `sessionStorage` (`tokenStorage.web.ts`).
- No navegador, a API só aceita chamadas de `http://localhost:8081`, configurado pela variável `CORS_ALLOWED_ORIGINS` do `.env`. Vazio desliga o CORS, como em produção.

Antes de abrir um PR do app:

```bash
npm run typecheck   # tsc --noEmit
npm run lint        # ESLint (eslint-config-expo)
npx expo-doctor     # versões compatíveis com o SDK
```

As rotas são tipadas (`typedRoutes` no `app.json`). O `npx expo start` gera os tipos em `.expo/types/`, que não vai para o git. Com o expo-router 58 o `typecheck` passa sem eles, então um clone novo não precisa rodar o `expo start` antes. Mesmo assim, os tipos gerados deixam o editor completar os caminhos de `Link` e `router.push`.

### CI (GitHub Actions)

Todo PR e todo push na `main` rodam três workflows, cada um só quando as pastas dele mudam:

- **Backend** (`.github/workflows/backend.yml`): `./mvnw test` com Java 17. Os testes de integração usam o Docker do runner, pelo Testcontainers. Quando um teste falha, os relatórios ficam como artefato da execução.
- **App** (`.github/workflows/app.yml`): `npm ci`, `typecheck`, `lint` e `npm test` com Node 24.
- **E2E** (`.github/workflows/e2e.yml`): a suíte do Playwright contra um banco só dela (ver "Testes e2e"), quando o app ou a API mudam.

Antes de abrir um PR, rode os mesmos comandos na sua máquina para não descobrir o erro só no CI.

### Testes unitários do app

Jest com o preset `jest-expo` e a [React Native Testing Library](https://callstack.github.io/react-native-testing-library/). Não precisam do backend nem do app no ar.

```bash
cd AccessMap.Front
npm test                     # roda todos
npm test -- --coverage       # com o relatório de cobertura em coverage/
```

- Ficam em `AccessMap.Front/__tests__/`, espelhando o `src/` (`utils`, `api`, `components`, `screens`, `hooks`), com o nome `*.test.ts(x)`.
- **API:** as chamadas são testadas com o `fetch` mockado.
- **Mocks em todos os testes** (`jest.setup.ts`): o `expo-secure-store` vira um mapa em memória e o `react-native-safe-area-context` usa o mock oficial dele.
- **Mocks por teste:** o `expo-router` e o `useAuth` são mockados em cada arquivo que precisa deles.
- **RNTL 14:** `render`, `fireEvent` e `renderHook` são assíncronos, então use sempre `await`.

### Testes e2e

Testam o app web de verdade, com cliques reais, usando o [Playwright](https://playwright.dev). Um arquivo por fluxo em `AccessMap.Front/e2e/`:

- `conta.spec.ts`: cadastro, login com senha errada, sair e entrar, troca de senha.
- `avaliacao.spec.ts`: avaliar, editar e excluir, permissões, volta ao local depois do login e validação do formulário.
- `navegacao.spec.ts`: menu ☰, barra inferior, Minhas avaliações e tela de erro (pela rota `dev/erro`, que só existe em desenvolvimento).
- `confiabilidade.spec.ts`, `review-oculta.spec.ts` e `datas.spec.ts`: selo, denúncias, fila de moderação, acesso do ADMIN e datas.
- `excluir-conta.spec.ts`: exclusão da própria conta.

Rodam num **ambiente só deles**, ao lado do de desenvolvimento e sem tocar nos seus dados:

| | Desenvolvimento | e2e |
|---|---|---|
| Banco | `accessmap-db`, porta 5432 | `accessmap-db-e2e` (profile `e2e` do `docker-compose.yml`), porta 5433, começa vazio a cada subida |
| API | 8080 | 8090, perfil `e2e` (`application-e2e.yaml`) |
| App web | 8081 | 8091, apontando para a API do e2e |

Com o Docker rodando, um comando sobe o banco, a API e o app, roda a suíte e derruba o banco no fim:

```bash
cd AccessMap.Front
npx playwright install chromium   # só na primeira vez
npm run e2e:ci                    # tudo; ou: npm run e2e:ci -- e2e/conta.spec.ts
```

- **Banco no ar:** se o banco do e2e já estiver de pé (`docker compose --profile e2e up -d --wait postgres-e2e`), o `npm run e2e` roda só o Playwright. A API e o app sobem pelo `webServer` do `playwright.config.ts`; se já estiverem nas portas 8090/8091, ele reaproveita.
- **Prints:** ficam em `AccessMap.Front/e2e-results/prints/`, fora do git.
- **Dados:** cada teste cria as próprias contas e locais. O que não dá para criar pela tela (idade da conta e papel ADMIN) é ajustado direto no banco do e2e. Variáveis para apontar para outro lugar: `E2E_DATABASE_URL`, `E2E_API_URL` e `E2E_APP_URL`.
- **Plataforma:** cobrem a versão web. O `Alert` e o layout do celular se conferem no Expo Go.
- **CI:** rodam no workflow **E2E** (`.github/workflows/e2e.yml`), com o Postgres como serviço do GitHub. O relatório, os prints e as capturas de falha ficam como artefato da execução.

### Deploy

A API vai para o Railway a partir do `AccessMap.Back/Dockerfile`, que sobe com o perfil `prod` (`application-prod.yaml`). O passo a passo, as variáveis de produção e os erros comuns estão em [`docs/deploy.md`](docs/deploy.md).

Para testar a imagem de produção na sua máquina, contra o Postgres local:

```bash
docker build -t accessmap-api AccessMap.Back
docker run --rm -p 8080:8080 -e JWT_SECRET=<32+ caracteres> \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/accessmap \
  -e DB_USERNAME=postgres -e DB_PASSWORD=accessmap123 accessmap-api
```

## 🔑 Chave da Google Maps Platform

A busca de locais usa a **Places API (New)**. Cada integrante gera a própria chave, sem custo dentro da cota gratuita:

1. Em https://console.cloud.google.com, crie um projeto (ex.: `accessmap-dev`) e vincule uma conta de faturamento. O Google exige isso mesmo para o uso gratuito.
2. Em **APIs e serviços → Biblioteca**, ative a **Places API (New)**. Atenção: não é a "Places API" antiga.
3. Em **APIs e serviços → Credenciais → Criar credenciais → Chave de API**, crie a chave e edite:
   - A chave começa com `AIza` e tem 39 caracteres. Credencial que começa com `AQ.` é de outro produto do Google Cloud e a Places recusa (401).
   - **Restrições de API**: marque só a **Places API (New)**. Se marcar a antiga, o Google responde 403 `API_KEY_SERVICE_BLOCKED`. Mudanças na chave levam alguns minutos para valer.
   - **Restrições de aplicativo**: deixe "Nenhuma" na chave de desenvolvimento. Restringir a um app Android ou iOS não funciona no Expo Go nem no navegador, porque o Expo Go se identifica para o Google como o app dele (`host.exp.exponent`), não como o AccessMap. A restrição por app fica para as chaves dos builds de produção.
4. Em **APIs e serviços → Places API (New) → Cotas**, limite as requisições por dia (ex.: 300). Em **Faturamento → Orçamentos e alertas**, crie um alerta de orçamento baixo.
5. Cole a chave em `AccessMap.Front/.env.local`, na linha `EXPO_PUBLIC_GOOGLE_PLACES_API_KEY=`, e reinicie o `npm start` / `npm run web`.

> Variáveis `EXPO_PUBLIC_*` vão dentro do app e qualquer pessoa pode extraí-las. Por isso a chave **nunca** vai para o git, e as restrições e a cota acima são obrigatórias. Sem a chave, a busca da Home mostra um aviso em vez de quebrar.

### Mapa e localização

A Home mostra os locais por perto em **lista** (padrão) ou **mapa**:

- **Expo Go**: o mapa (`react-native-maps`) funciona sem configurar nada. É Google Maps no Android e Apple Maps no iOS, usando a configuração do próprio Expo Go. A chave acima serve para a lista de locais, não para desenhar o mapa.
- **Navegador**: não tem mapa, porque o `react-native-maps` não roda na web. A Home mostra só a lista, e o seletor Lista | Mapa não aparece.
- **Build de loja** (EAS): o Android precisa de uma chave própria do **Maps SDK for Android**, configurada no plugin do `react-native-maps` no `app.json`. Isso fica para quando houver build de produção.
- **Localização no celular**: o app pede permissão na Home. No Expo Go o celular precisa estar na mesma rede Wi-Fi do computador; o endereço da API é descoberto pelo IP do Metro, sem configurar nada (veja o `AccessMap.Front/.env.example`).

## 👥 Equipe Desenvolvedora
* João Victor Martins
* Gustavo Borges Hertz
* Gabriel Maurity
* Gabriel Rezende

---

## 📖 Guia de Contribuição e Boas Práticas

Para mantermos o código organizado e facilitarmos a revisão, adotamos as seguintes regras de versionamento:

### 1. Nomenclatura de Branches
Crie sempre uma branch específica para a sua tarefa a partir da branch principal (`main` ou `develop`). O padrão de nomenclatura deve seguir o formato: `tipo/escopo/descricao-curta`.

**Tipos permitidos:**
* `feat/`: Para novas funcionalidades.
* `fix/`: Para correção de bugs.
* `docs/`: Para alterações em documentações (como este README ou Swagger).
* `refactor/`: Para refatoração de código que não altera o comportamento.

**Exemplos práticos:**
* `feat/back/setup-spring` (Configuração inicial do backend)
* `feat/front/mapa-inicial` (Criação do componente de mapa no React)
* `fix/mobile/erro-login` (Correção na tela de login do app)

---

### 2. Padrão de Commits (Conventional Commits)
As mensagens de commit devem ser claras, descritivas e seguir os prefixos do padrão *Conventional Commits*:

| Prefixo | Quando usar |
| :--- | :--- |
| `feat:` | Criação de uma nova funcionalidade (ex: novo endpoint, novo componente) |
| `fix:` | Correção de erro ou bug no código |
| `docs:` | Mudança exclusiva em arquivos de documentação (`README.md`, comentários, Swagger) |
| `refactor:` | Reestruturação de código (não altera funcionalidades e não corrige bugs) |
| `test:` | Criação ou ajuste em testes automatizados (JUnit, Jest, Cypress) |
| `chore:` | Configurações gerais, dependências, arquivos de build ou CI/CD (ex: `pom.xml`, `.gitignore`) |

#### 💻 Comandos e Exemplos Práticos de Posição no Terminal

**`docs:` — Alterando ou criando documentações**
```bash
# Exemplo 1: Adicionando ou atualizando este README
git commit -m "docs: adiciona guia de contribuicao e regras de branches ao README"

# Exemplo 2: Documentando um contrato de endpoint no Swagger
git commit -m "docs(api): documenta endpoint de cadastro de pontos no swagger"

```

**`feat:` — Criando novas funcionalidades**

```bash
# Exemplo 1: Criando uma entidade ou service no back-end
git commit -m "feat(back): implementa service de calculo de rotas acessiveis"

# Exemplo 2: Criando uma tela ou componente no front-end
git commit -m "feat(web): adiciona modal de reporte de barreiras urbanas"

```

**`fix:` — Corrigindo problemas**

```bash
# Exemplo 1: Corrigindo uma validação na API
git commit -m "fix(back): corrige validacao de campos vazios no DTO de reporte"

# Exemplo 2: Consertando visualização no app mobile
git commit -m "fix(mobile): resolve erro de renderizacao do mapa em dispositivos iOS"

```

**`chore:` / `refactor:` / `test:` — Outros comandos de rotina**

```bash
# Ajustando dependências (ex: pom.xml ou package.json)
git commit -m "chore(back): adiciona dependencia do Hibernate Spatial ao pom.xml"

# Refatorando um código existente sem alterar a funcionalidade
git commit -m "refactor(back): reorganiza pacotes da camada de controllers"

# Criando testes de unidade
git commit -m "test(back): adiciona teste unitario para o service de validacao"

```

---

### 3. Atenção às Credenciais do Git

Antes de realizar pushes, certifique-se de que está utilizando as credenciais corretas no terminal (`user.name` e `user.email`). Isso é essencial caso alguém do grupo precise compartilhar o mesmo computador para subir código ou se você estiver alternando entre sua conta do GitHub pessoal e a da faculdade.

* Verifique com: `git config user.email` e `git config user.name`.
* Defina localmente se necessário: `git config user.name "Seu Nome"` e `git config user.email "seu@email.com"`.

---

### 4. Regras para Pull Requests (PRs)

Nunca faça push direto na branch `main`. Todo código deve ser integrado via Pull Request.

1. Suba sua branch: `git push origin feat/sua-branch`.
2. Abra o PR no GitHub apontando para a branch principal.
3. No corpo do PR, descreva o que foi feito:
* *O que este PR resolve?*
* *Quais endpoints/telas foram alterados?*


4. O PR deve ser revisado e aprovado por pelo menos **um** outro integrante do grupo antes de ser mergeado.

---

## 🚦 Próximos Passos (Em Desenvolvimento)

1. Estruturação do repositório base.
2. Definição e documentação dos contratos de API (Endpoints).
3. Configuração do banco de dados geoespacial.
