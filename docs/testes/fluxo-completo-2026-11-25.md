# Teste de fluxo completo — 2026-11-25

## Ambiente
- **Back:** Spring Boot rodando pelo IntelliJ (porta 8080), Flyway em V11, Postgres no Docker (`accessmap-db`). O log do back não foi lido; a análise usou as respostas HTTP e o banco.
- **Front:** Expo SDK 58 / RN 0.88.1 na web (`expo start --web`, porta 8081), testado no navegador do Claude e depois em viewport mobile (375×812).
- **Mapa nativo:** não testado (decisão: só web).
- **Contas de teste:** duas contas fictícias criadas no Postgres local só para este teste.
- **Local usado:** Ibirapuera Shopping (`ChIJ04IdFglazpQRinICx3xqoO4`).

## Correções (branch `fix/correcao-geral-fluxo`)
| Achado | Situação | Commit |
|--------|----------|--------|
| A1 IP fixo da API | Corrigido: a URL vem do host da página (web) ou do Metro (Expo Go) | `fix(mobile): app acha a API sozinho e desiste em 15 s` |
| A2 sem timeout | Corrigido: 15 s, conferido com API inacessível (erro em ~15 s) | idem |
| A3 menu ☰ | Corrigido: painel com Perfil, Minhas avaliações, Moderação (ADMIN) e Sair | `feat(mobile): menu, barra inferior e Minhas avaliacoes` |
| A4 barra inferior | Corrigido: Home, Restaurantes e Review funcionam | idem |
| A5 excluir sem histórico | Corrigido: volta para o local | `fix(mobile): navegacao da avaliacao e volta ao local depois do login` |
| A6 chave Maps SDK Android | Pendente: decidir no aparelho (já citado no README para build de loja) | — |
| A7 localização negada na web | Corrigido: explica como liberar e oferece "Tentar de novo" | `feat(mobile): tela de erro amigavel e avisos de localizacao e voltar` |
| A8 formulário de local já avaliado | Corrigido: redireciona para a avaliação existente, com aviso | `fix(mobile): navegacao da avaliacao ...` |
| A9 Minhas avaliações | Corrigido: `/minhas-avaliacoes` com o nome do local (V12 no back) | `feat(review): guarda o nome do local ...` |
| A10 typecheck | Corrigido: 0 erros | `fix(mobile): typecheck limpo com os tipos do RN 0.88` |
| A11 e2e | Corrigido: 25 testes passando, 3 specs novos (#137) | `test(e2e): fluxos principais ...` |
| A12 voltar "index" | Corrigido, e também o "(auth)/login" do cadastro | `feat(mobile): tela de erro ...` e `fix(mobile): voltar do cadastro ...` |

Também entraram: #102 (volta ao local depois do login), #133 (tela de erro) e #137 (e2e dos fluxos principais).

## Resultado por item
| # | Item | Resultado |
|---|------|-----------|
| 1 | Cadastro e tempo de gravação | **OK depois de corrigir a URL** (A1/A2 bloqueavam). 201 em 450 ms; linha no banco ~360 ms após o clique, `created_at` em UTC. |
| 2 | Mapa / chave da API | **OK na web.** Autocomplete 200 (~0,9 s), `searchNearby` 200 (~0,4 s). Mapa nativo não testado (A6). Sem feedback quando a localização é negada (A7). |
| 3 | Login após cadastro | **OK.** Cadastro já entra logado; logout; login 401 com senha errada; 429 após 5 erros com mensagem clara; sessão sobrevive ao recarregar. |
| 4 | Uma review por usuário por lugar | **OK.** Duplo clique → 1 POST; segunda review → 409; 4 POSTs simultâneos → 1×201 + 3×409; editar, excluir e reavaliar funcionam; segundo usuário avalia o mesmo local. Problemas de navegação em A5 e A8. |
| 5 | Tela de perfil | **OK.** Dados corretos; edição persiste (nome, e-mail, idade, necessidades); conflito de telefone → 409 "Telefone já está em uso"; troca de senha valida a senha atual e encerra as sessões. Falta "Minhas avaliações" (A9). |
| 6 | Menu hambúrguer | **FALHOU.** O ☰ não faz nada (A3). A barra inferior também não (A4). |

## Achados (para o plano de correção)

### Bloqueia

#### A1 — `EXPO_PUBLIC_API_URL` com IP fixo e desatualizado
- **Obtido:** `.env.local` apontava para `http://192.168.1.9:8080`; o IP do PC era `192.168.1.16`. O cadastro não chegava ao back e nada era gravado.
- **Arquivo:** `AccessMap.Front/.env.local` e `src/api/client.ts` (`BASE_URL`).
- **Correção sugerida:**
  - Na web, usar `http://localhost:8080`.
  - No celular, derivar o host do Metro (`Constants.expoConfig?.hostUri`) e usar o `.env` só como override.
- **Feito durante o teste:** `.env.local` trocado para `192.168.1.16` e o Expo reiniciado.

#### A3 — Menu hambúrguer não implementado
- **Arquivo:** `src/screens/HomeScreen.tsx` ~l.71–74.
- **Obtido:** `TouchableOpacity` sem `onPress`, sem `accessibilityRole`/`accessibilityLabel`. O clique não faz nada e o leitor de tela não anuncia o botão.
- **Correção sugerida:** menu (modal/drawer) com Perfil, Minhas avaliações, Moderação (só ADMIN) e Sair. Usar as rotas existentes (`/perfil`, `/moderacao`) e o `signOut` do `AuthContext`.

#### A4 — Barra inferior (Home / Restaurantes / Review) não navega
- **Arquivo:** `src/screens/HomeScreen.tsx` ~l.194–206.
- **Obtido:** três `TouchableOpacity` sem `onPress`. O clique não faz nada.
- **Correção sugerida:** implementar (por exemplo, Restaurantes = filtro de categoria; Review = minhas avaliações) ou remover até existir destino. Definir junto com o A3 para não duplicar a navegação.

### Atrapalha

#### A2 — Requisições sem tempo limite
- **Arquivo:** `src/api/client.ts` (`send` chama `fetch` sem timeout).
- **Obtido:** com a API inacessível, o botão fica girando por dezenas de segundos até o navegador desistir. Só então aparece "Não foi possível conectar ao servidor".
- **Correção sugerida:** `AbortSignal.timeout(15000)` e a mesma mensagem.

#### A5 — Depois de excluir uma avaliação, a tela não sai do lugar
- **Arquivo:** `src/screens/ReviewDetailScreen.tsx` l.61–62 (`router.back()`).
- **Passo:** abrir a avaliação sem histórico para voltar (link direto ou redirecionamento depois do 409) e excluir.
- **Obtido:** `DELETE` 200 e status `REMOVIDA` no banco, mas a tela continua mostrando a avaliação apagada com o botão "Editar".
- **Correção sugerida:** `router.canGoBack() ? router.back() : router.replace('/locais/[placeId]' ...)`.

#### A6 — Mapa nativo sem chave do Google Maps SDK (não testado)
- **Arquivo:** `app.json`.
- **Problema:** não tem `android.config.googleMaps.apiKey`, e o `MapView` em `src/components/PlacesMap.tsx` usa o provider padrão. No Android o mapa deve aparecer em branco ou dar erro em build de produção.
- **Situação:** no Expo Go funciona com a chave do próprio Expo. Testar no aparelho antes de decidir.
- **iOS:** usa Apple Maps e não precisa de chave.

#### A7 — "Permitir localização" sem retorno quando a permissão está negada
- **Arquivo:** `src/screens/HomeScreen.tsx` e o hook de localização.
- **Obtido:** com a geolocalização negada no navegador, o clique não faz nada visível.
- **Correção sugerida:** mensagem orientando a liberar nas configurações do navegador ou do aparelho.

#### A8 — Formulário de avaliação abre mesmo quando o usuário já avaliou
- **Arquivo:** `src/screens/ReviewFormScreen.tsx`.
- **Passo:** abrir `/locais/[placeId]/avaliar` direto (link ou voltar no histórico) para um local já avaliado.
- **Obtido:** o usuário preenche tudo, recebe 409 e é levado à avaliação existente. O que preencheu se perde. A regra funciona; só a experiência é ruim.
- **Correção sugerida:** consultar `GET /api/reviews?placeId&userId` ao abrir o formulário e redirecionar antes.

#### A9 — Não há "Minhas avaliações"
- **Problema:** o perfil não lista as avaliações do usuário. O back já suporta: `GET /api/reviews?userId=<me>`.
- **Correção sugerida:** seção no perfil ou item do menu (A3).

#### A10 — Erros de typecheck depois do upgrade para RN 0.88
- **Comando:** `npx tsc --noEmit` dá 9 erros.
  - `ListHeaderComponent`/`ListEmptyComponent` recebendo `null`:
    - `ModerationScreen.tsx` l.119–120
    - `ReviewListScreen.tsx` l.81, 89, 90
    - `NearbyPlaceList.tsx`
  - Tipo do `ref` de `ScrollView`: `ReviewFormScreen.tsx` l.39 e l.124.
- **Situação:** o app roda (a web compila), mas a CI/typecheck quebra.
- **Correção sugerida:** trocar `null` por `undefined` e tipar o ref com o novo `ScrollViewInstance`.
- **Lint e `expo-doctor`:** limpos (20/20).

#### A11 — Suíte e2e não roda
- **Problema:** o Playwright foi atualizado e o navegador dele não está instalado (`chromium_headless_shell-1255`). Os 9 testes falharam antes de abrir a página.
- **Correção:** `npx playwright install chromium` e rodar `npm run e2e` de novo.

### Cosmético

#### A12 — Botão voltar com rótulo "index, back"
- **Problema:** no detalhe do local, o voltar para a home é anunciado como "index". Falta `title` na rota `index` em `src/app/_layout.tsx`.
- **Também:** abrindo uma página por link direto não há botão voltar no cabeçalho.

## Funcionou bem
- **Validações de cadastro:** nome, telefone 10–11 dígitos, senha ≥ 8.
- **Duplo clique:** não duplica cadastro nem avaliação.
- **Mensagens de erro do back:** todas apareceram em português na UI (409, 401, 429, 400).
- **Médias e contagens por característica:** atualizam depois de criar, editar e excluir.
- **Layout mobile (375 px):** sem rolagem horizontal; telas de home e local legíveis.
- **Console do navegador e log do Metro:** sem erros durante o teste.

## Ordem sugerida para o plano de correção
1. **A1 + A2:** conexão confiável com a API (resolve "cadastro não grava").
2. **A3 + A4 + A9:** navegação (menu, barra inferior, minhas avaliações).
3. **A10 + A11:** typecheck e e2e de volta ao verde, para proteger as próximas mudanças.
4. **A5 + A8:** fluxo de avaliação.
5. **A6 + A7:** mapa e localização, testados no aparelho.
6. **A12:** cosmético.
