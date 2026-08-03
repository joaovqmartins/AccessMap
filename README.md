# ♿ AccessMap

O AccessMap é uma plataforma composta por um aplicativo mobile e um portal web que permite à comunidade mapear colaborativamente pontos de acessibilidade e barreiras urbanas. 

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

**Front-end (Web e Mobile)**
* React com TypeScript e Tailwind CSS (Portal Web)
* Leaflet.js (Mapas interativos)
* React Native com Expo (Aplicativo Mobile)
* Jest e Cypress para testes

**DevOps & CI/CD**
* Git & GitHub Actions
* Deploy contínuo na plataforma Railway

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
