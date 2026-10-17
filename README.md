# ♿ AccessMap

O AccessMap é um aplicativo mobile que permite à comunidade mapear colaborativamente pontos de acessibilidade e barreiras urbanas. 

O objetivo é fornecer informações reais e atualizadas sobre a acessibilidade dos espaços urbanos, garantindo maior autonomia para pessoas com deficiência ou mobilidade reduzida, além de gerar dados abertos para o embasamento de políticas públicas de inclusão.

## 🚀 Funcionalidades Principais
* **Mapeamento Colaborativo:** Reporte de barreiras (ex: calçadas danificadas, ausência de rampas) e pontos de acessibilidade, com anexo de fotos e localização exata.
* **Validação Comunitária:** Sistema de confirmação coletiva para garantir a confiabilidade das informações.
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

**Por que a aplicação não roda no Docker?** Uma *imagem* é uma "foto" pronta do programa e um *container* é essa foto rodando. Se a API rodasse num container, cada mudança no código exigiria gerar a foto de novo (`docker build`). No dia a dia é mais rápido rodar pela IDE, com debugger e recarga. Por isso o [`docker-compose.yml`](docker-compose.yml) tem **só dependências**. O [`backend/Dockerfile`](backend/Dockerfile) continua no repositório, mas é usado só no deploy.

### 1. Variáveis de ambiente (uma vez)

São dois arquivos, cada um com o passo a passo nos comentários. Os dois ficam fora do git.

```bash
cp .env.example .env                        # backend: preencha o JWT_SECRET
cp frontend/.env.example frontend/.env.local  # app: preencha a chave do Google (seção abaixo)
```

### 2. Backend

Pré-requisitos: Docker aberto e Java 17+.

1. Abra o projeto no IntelliJ e rode o `BackendApplication` (▶ ao lado do `main`). O diretório de trabalho é a pasta `backend/`, que é o padrão do IntelliJ para o módulo Maven.
2. Ao subir, o Spring Boot:
   - lê o `.env` da raiz sozinho, sem precisar configurar variáveis na IDE;
   - roda `docker compose up` e espera o Postgres ficar pronto (`spring-boot-docker-compose`);
   - aplica as migrações do Flyway.
3. Ao parar a API, o banco continua rodando. Os dados ficam no volume `pgdata` e só somem com `docker compose down -v`.

Pelo terminal funciona igual: `cd backend && ./mvnw spring-boot:run`.

Swagger: http://localhost:8080/swagger-ui.html — use **Authorize** com o `accessToken` de `/api/auth/login`.
Para ver os dados, conecte no banco **`accessmap`** (`localhost:5432`, usuário `postgres`, senha `accessmap123`).

Testes (`./mvnw test`) sobem um Postgres descartável via Testcontainers e precisam do Docker rodando.

### 3. App

Pré-requisitos: Node 20+, backend rodando e, para o celular, o Expo Go.

```bash
cd frontend
npm ci
npm start       # QR code para o Expo Go; "a" abre no emulador Android
npm run web     # navegador em http://localhost:8081
```

- O `EXPO_PUBLIC_API_URL` do `frontend/.env.local` muda conforme onde o app roda; os valores estão explicados no arquivo. O Expo só lê esse arquivo ao iniciar.
- No celular e no emulador, o refresh token fica no armazenamento seguro do aparelho (`expo-secure-store`). No navegador ele fica no `sessionStorage` (`tokenStorage.web.ts`).
- No navegador, a API só aceita chamadas de `http://localhost:8081`, configurado pela variável `CORS_ALLOWED_ORIGINS` do `.env`. Vazio desliga o CORS, como em produção.

Antes de abrir um PR do app:

```bash
npm run typecheck   # tsc --noEmit
npm run lint        # ESLint (eslint-config-expo)
npx expo-doctor     # versões compatíveis com o SDK
```

As rotas são tipadas (`typedRoutes` no `app.json`): os tipos ficam em `.expo/types/`, que não vai para o git e só é gerado pelo `npx expo start`. Num clone novo, ou depois de criar uma rota, rode o `expo start` uma vez antes do `typecheck`; senão o `tsc` acusa erro em todo `Link`/`router.push`.

## 🔑 Chave da Google Maps Platform

A busca de locais usa a **Places API (New)**. Cada integrante gera a própria chave, sem custo dentro da cota gratuita:

1. Em https://console.cloud.google.com, crie um projeto (ex.: `accessmap-dev`) e vincule uma conta de faturamento. O Google exige isso mesmo para o uso gratuito.
2. Em **APIs e serviços → Biblioteca**, ative a **Places API (New)**. Atenção: não é a "Places API" antiga.
3. Em **APIs e serviços → Credenciais → Criar credenciais → Chave de API**, crie a chave e edite:
   - A chave começa com `AIza` e tem 39 caracteres. Credencial que começa com `AQ.` é de outro produto do Google Cloud e a Places recusa (401).
   - **Restrições de API**: marque só a **Places API (New)**. Se marcar a antiga, o Google responde 403 `API_KEY_SERVICE_BLOCKED`. Mudanças na chave levam alguns minutos para valer.
   - **Restrições de aplicativo**: deixe "Nenhuma" na chave de desenvolvimento. Restringir a um app Android ou iOS não funciona no Expo Go nem no navegador, porque o Expo Go se identifica para o Google como o app dele (`host.exp.exponent`), não como o AccessMap. A restrição por app fica para as chaves dos builds de produção.
4. Em **APIs e serviços → Places API (New) → Cotas**, limite as requisições por dia (ex.: 300). Em **Faturamento → Orçamentos e alertas**, crie um alerta de orçamento baixo.
5. Cole a chave em `frontend/.env.local`, na linha `EXPO_PUBLIC_GOOGLE_PLACES_API_KEY=`, e reinicie o `npm start` / `npm run web`.

> Variáveis `EXPO_PUBLIC_*` vão dentro do app e qualquer pessoa pode extraí-las. Por isso a chave **nunca** vai para o git, e as restrições e a cota acima são obrigatórias. Sem a chave, a busca da Home mostra um aviso em vez de quebrar.

### Mapa e localização

A Home mostra os locais por perto em **lista** (padrão) ou **mapa**:

- **Expo Go**: o mapa (`react-native-maps`) funciona sem configurar nada. É Google Maps no Android e Apple Maps no iOS, usando a configuração do próprio Expo Go. A chave acima serve para a lista de locais, não para desenhar o mapa.
- **Navegador**: não tem mapa, porque o `react-native-maps` não roda na web. A Home mostra só a lista, e o seletor Lista | Mapa não aparece.
- **Build de loja** (EAS): o Android precisa de uma chave própria do **Maps SDK for Android**, configurada no plugin do `react-native-maps` no `app.json`. Isso fica para quando houver build de produção.
- **Localização no celular**: o app pede permissão na Home. Para testar no Expo Go, o `EXPO_PUBLIC_API_URL` precisa ser o IP do computador na rede (ex.: `http://192.168.1.9:8080`), não `localhost`. Veja o `frontend/.env.example`.

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
