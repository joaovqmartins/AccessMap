# Regressão manual antes da entrega (#57)

Os testes automatizados cobrem a API (testes de integração), as telas (Jest) e os fluxos no navegador (Playwright, também no CI). Eles não substituem usar o app **no celular**, com o teclado, a localização e o leitor de tela reais. Esta é a rodada manual dos fluxos principais, com evidência, como a disciplina pede.

- **Quando rodar:** antes de cada entrega e depois de qualquer mudança grande no app.
- **Quanto tempo leva:** ~45 min para o roteiro completo e ~5 min de vídeo.

## 1. Preparação

### Ambiente

| Item | Como |
|---|---|
| Backend | Local (`./mvnw spring-boot:run` em `AccessMap.Back`, com o Postgres do `docker compose up -d`) ou a API publicada no Render (`https://accessmap-7aow.onrender.com`) |
| App | `npm start` em `AccessMap.Front` e abrir no **Expo Go** do celular. O `EXPO_PUBLIC_API_URL` do `.env.local` precisa apontar para um endereço que o celular alcance: o IP do computador na rede (não `localhost`) ou o endereço do Render |
| Celular | Localização ligada, e um segundo aparelho (ou o navegador) para a parte de denúncias |

### Contas de teste

Crie as contas pelo próprio app (tela de cadastro) e ajuste no banco o que o app não deixa fazer, como envelhecer a conta ou torná-la ADMIN. Use telefones fictícios, nunca o de uma pessoa real.

| Conta | Para quê | Ajuste no banco |
|---|---|---|
| **A**, nova | Cadastro, avaliação, selo "conta nova" | Nenhum |
| **B**, **C** e **D**, estabelecidas | As três denúncias que ocultam uma avaliação | Idade de 40 dias |
| **E**, ADMIN | Fila de moderação | Idade de 40 dias e papel ADMIN |

No banco local, troque `<telefone>` pelo telefone da conta (só os 11 dígitos):

```powershell
docker exec -it accessmap-db psql -U postgres -d accessmap
```

```sql
-- Conta estabelecida: passou dos 7 dias de conta nova, então as denúncias dela contam
UPDATE users SET created_at = now() - interval '40 days' WHERE phone = '<telefone>';
-- ADMIN
UPDATE users SET role = 'ADMIN', created_at = now() - interval '40 days' WHERE phone = '<telefone>';
```

O ADMIN também pode ser criado na primeira subida pelas variáveis `ADMIN_PHONE` e `ADMIN_PASSWORD` (veja o [guia do deploy](../deploy.md)).

## 2. Roteiro

Marque ✅ ou ❌. Na coluna de evidência, anote o nome do print (em `docs/testes/evidencias/`) ou o minuto do vídeo. A última coluna diz se o fluxo também é coberto por um teste automatizado.

### 2.1 Cadastro e login

| # | Ação | Esperado | Resultado | Evidência | Automatizado |
|---|---|---|---|---|---|
| 1 | Cadastrar a conta A com o telefone vazio ou incompleto | Erro no campo, sem enviar | | | `conta.spec.ts` |
| 2 | Cadastrar a conta A com dados válidos | Entra direto na tela inicial, já logado | | | `conta.spec.ts` |
| 3 | Sair pelo menu ☰ e entrar com a senha errada | "Telefone ou senha incorretos" | | | `conta.spec.ts` |
| 4 | Entrar com a senha certa | Volta para a tela inicial, logado | | | `conta.spec.ts` |
| 5 | Perfil → trocar a senha | A sessão encerra e o app pede a senha nova | | | `conta.spec.ts` |

### 2.2 Busca de local

| # | Ação | Esperado | Resultado | Evidência | Automatizado |
|---|---|---|---|---|---|
| 6 | Permitir a localização quando o app pedir | Lista de locais próximos, com a distância | | | — (precisa de GPS real) |
| 7 | Negar a localização (em outra tentativa ou nos ajustes do celular) | Mensagem explicando, sem travar a tela | | | — |
| 8 | Buscar um local pelo nome | Sugestões do Google; tocar abre a tela do local com o mapa no topo | | | Parcial (`avaliacao.spec.ts`) |
| 9 | Filtros → marcar uma característica (ex.: Rampas e entradas) → Aplicar | O botão mostra "Filtros (1)" e a lista mostra "Filtrando por: …" | | | — |
| 10 | Filtros → "Minhas necessidades" | A lista considera as necessidades do perfil | | | — |
| 11 | Barra inferior → Restaurantes | Seleciona a categoria | | | `navegacao.spec.ts` |

### 2.3 Avaliação

| # | Ação | Esperado | Resultado | Evidência | Automatizado |
|---|---|---|---|---|---|
| 12 | Sem login, abrir um local → "Entrar para avaliar" | Vai para o login e, depois de entrar, volta ao formulário do local | | | `avaliacao.spec.ts` |
| 13 | Enviar o formulário sem nota e sem característica | Erros nos dois campos | | | `avaliacao.spec.ts` |
| 14 | Avaliar com nota, características e comentário | A avaliação aparece no local, e a nota e as características do local mudam | | | `avaliacao.spec.ts` |
| 15 | Abrir "Avaliar" de novo no mesmo local | Vai direto para a própria avaliação, sem formulário duplicado | | | `avaliacao.spec.ts` |
| 16 | Editar a nota | O local mostra a nota nova | | | `avaliacao.spec.ts` |
| 17 | Menu ☰ → Minhas avaliações | A avaliação aparece com o nome do local | | | `navegacao.spec.ts` |
| 18 | Excluir a avaliação | Some do local e da lista | | | `avaliacao.spec.ts` |

### 2.4 Denúncia e confiabilidade

Use uma avaliação **da conta A** (nova) num local que você vai abrir com as contas B, C e D.

| # | Ação | Esperado | Resultado | Evidência | Automatizado |
|---|---|---|---|---|---|
| 19 | Ver a avaliação da conta A, por qualquer conta | Selo "Avaliado por conta nova" | | | `confiabilidade.spec.ts` |
| 20 | Com a conta B, denunciar a avaliação | "Denúncia enviada"; denunciar de novo não é possível | | | `confiabilidade.spec.ts` |
| 21 | Com as contas C e D, denunciar também | Na terceira, a avaliação some do local e a nota do local muda | | | `confiabilidade.spec.ts` |
| 22 | Com a conta A, abrir a própria avaliação | Aparece "em análise", sem Editar, com Excluir | | | `review-oculta.spec.ts` |
| 23 | Com uma conta Confiável, se houver (ver [confiabilidade](../confiabilidade.md)) | Selo "✓ Avaliador confiável" | | | `confiabilidade.spec.ts` |

### 2.5 Moderação (ADMIN)

| # | Ação | Esperado | Resultado | Evidência | Automatizado |
|---|---|---|---|---|---|
| 24 | Com a conta A, B, C ou D, abrir o menu ☰ | Não aparece "Moderação" | | | `confiabilidade.spec.ts` |
| 25 | Com a conta E (ADMIN), menu ☰ → Moderação | A avaliação do passo 21 aparece como "Oculta · Denúncias (3)" | | | `confiabilidade.spec.ts` |
| 26 | Restaurar | Sai da fila e volta para o local | | | `confiabilidade.spec.ts` |
| 27 | Remover outra avaliação da fila (se houver) | Some para todos | | | `confiabilidade.spec.ts` |

### 2.6 Conta

| # | Ação | Esperado | Resultado | Evidência | Automatizado |
|---|---|---|---|---|---|
| 28 | Perfil → editar nome e necessidades | O perfil mostra os dados novos | | | — |
| 29 | Perfil → Excluir minha conta, com a senha errada | "Senha atual incorreta" | | | `excluir-conta.spec.ts` |
| 30 | Excluir com a senha certa (use uma conta descartável) | "Conta excluída", volta ao login, e o login antigo não funciona mais | | | `excluir-conta.spec.ts` |

### 2.7 Acessibilidade no aparelho

Os pontos principais do [roteiro da auditoria](../acessibilidade/auditoria.md#roteiro-manual-no-aparelho). Para a rodada completa (iOS e Android), use o roteiro de lá.

| # | Ação | Esperado | Resultado | Evidência | Automatizado |
|---|---|---|---|---|---|
| 31 | Com TalkBack ou VoiceOver, percorrer a tela inicial | A ordem segue a tela; o ☰ é "Abrir menu" | | | Parcial (`a11y.spec.ts`) |
| 32 | No formulário, tocar nas estrelas | É lido "4 estrelas, selecionado" | | | Parcial (`a11y.spec.ts`) |
| 33 | Enviar o login vazio | O erro é anunciado | | | Parcial (`a11y.spec.ts`) |
| 34 | Fonte do sistema no máximo | Nada cortado na tela inicial, no local, no formulário e no perfil | | | — |

## 3. Vídeo do fluxo completo

Grave a tela do celular (gravador nativo do Android ou do iOS) em **um vídeo de ~5 min**, nesta ordem:

1. **Cadastro** da conta A (passo 2).
2. **Busca** com a localização e um filtro (passos 6 e 9).
3. **Avaliação** de um local com nota, características e comentário, e o local atualizado (passo 14).
4. **Selo** "conta nova" na avaliação (passo 19).
5. **Denúncia** com uma conta estabelecida (passo 20). As outras duas podem ser feitas antes da gravação, pelo navegador.
6. **Moderação:** a fila com a avaliação oculta e Restaurar (passos 25 e 26).
7. **Perfil** e sair.

O vídeo **não entra no git** por causa do tamanho. Suba no Drive do grupo (ou no YouTube, como não listado) e coloque o link na seção 4. Os prints vão em `docs/testes/evidencias/`, nomeados pelo número do passo (ex.: `14-avaliacao-publicada.png`).

## 4. Resultado da rodada

| Campo | Valor |
|---|---|
| Data | |
| Quem rodou | |
| Aparelho e sistema | |
| Backend | Local ou Render |
| Versão (commit da `main`) | |
| Vídeo | |
| Passos ✅ | /34 |

### Problemas encontrados

Para cada ❌: o passo, o que aconteceu, o print e a issue aberta.

| Passo | O que aconteceu | Evidência | Issue |
|---|---|---|---|
| | | | |
