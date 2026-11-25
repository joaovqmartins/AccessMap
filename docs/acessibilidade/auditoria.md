# Auditoria de acessibilidade do app — 2026-11-25 (#56)

O AccessMap existe para ajudar pessoas com deficiência e mobilidade reduzida, então o próprio app precisa ser acessível. A referência é o **WCAG 2.1, nível AA**.

## Como foi feita
1. **Contraste das cores (automático):** `__tests__/a11y/contraste.test.ts` (Jest) calcula a razão de contraste de cada par texto/fundo e de cada borda de campo do tema (`src/constants/theme.ts`).
   - Texto: 4,5:1.
   - Bordas de campo e componentes de interface: 3:1 (WCAG 1.4.11).
2. **Regras WCAG nas telas (automático):** `e2e/a11y.spec.ts` (Playwright + axe-core) roda as regras `wcag2a`, `wcag2aa`, `wcag21a` e `wcag21aa` na versão web. Telas cobertas:
   - login e cadastro
   - início, menu e filtros
   - perfil, detalhes do local e formulário de avaliação
   - Minhas avaliações

   O teste falha em violações `serious` e `critical`.
3. **Varredura do código:**
   - cores fora do tema;
   - tamanhos de fonte;
   - botões sem papel ou sem nome;
   - alvos de toque.
4. **Leitor de tela e fonte grande (manual, no aparelho):** roteiro abaixo.

## Achados e correções

| # | Achado | Critério WCAG | Severidade | Correção |
|---|--------|---------------|------------|----------|
| 1 | Azul principal `#007BFF` com 3,98:1 no branco: botões, links, abas e chips selecionados | 1.4.3 Contraste | Alta | `colors.primary` virou `#0062CC` (5,8:1) |
| 2 | Texto secundário `#6B7280` com 4,43:1 no fundo cinza | 1.4.3 | Média | `colors.textMuted` virou `#5B6270` (≥ 5,3:1 em todos os fundos do app) |
| 3 | Cor de aviso `#B26A00` com 4,24:1 no branco | 1.4.3 | Média | `colors.warning` virou `#9A5B00` (5,4:1) |
| 4 | Borda dos campos de texto `#E0E0E0` com 1,3:1; o campo de busca não tinha borda (cinza sobre branco, 1,1:1) | 1.4.11 Contraste não textual | Alta | Nova cor `colors.inputBorder` `#8C8C8C` (3,4:1) nos campos e no campo de busca |
| 5 | 17 cores soltas fora do tema (Home, Perfil, busca, mensagem de erro, menu) | Manutenção | Média | Todas viraram nomes do tema (`surfaceMuted`, `dangerBackground`, `infoBackground`, `overlay`…). Só os pinos do mapa seguem as cores nativas do Android, com legenda em texto |
| 6 | Textos de 11–12 px (chips das avaliações, atribuição do Google, legenda do mapa) | 1.4.4 / legibilidade | Média | Novo mínimo `MIN_FONT_SIZE = 13` |
| 7 | Avatar mostrava as iniciais, mas se chamava só "Meu perfil" | 2.5.3 Rótulo no nome | Alta (axe: serious) | Nome "Meu perfil, PE": inclui as iniciais visíveis, sem parênteses, que o leitor de tela trataria como comentário |
| 8 | Aba de baixo mostrava "Review", mas se chamava "Minhas avaliações": quem usa controle por voz fala "Review" e nada acontece | 2.5.3 | Alta (axe: serious) | Nome "Review: minhas avaliações" |
| 9 | Menu com `aria-modal` sem papel de diálogo (atributo inválido) | 4.1.2 Nome, função, valor | Alta (axe: critical) | `role="dialog"` com o nome "Menu" |
| 10 | Estrelas da nota, opções das características, necessidades e filtros (`radio`/`checkbox`) sem `aria-checked` na web: o leitor de tela não dizia o que estava marcado | 4.1.2 | Alta (axe: critical) | `aria-checked` junto do `accessibilityState` |
| 11 | Abas de baixo e chips de categoria sem estado selecionado na web | 4.1.2 | Média | `aria-selected` / `aria-pressed` (já entrou com o menu, #142) |

**Já existia e foi mantido:**
- alvo de toque mínimo de 44 px (`MIN_TOUCH`);
- avaliações de característica sempre em texto, além da cor;
- estrela vazia em contorno (☆), para a nota não depender só da cor;
- erros com `accessibilityLiveRegion`;
- rótulos nos campos (`TextField`).

**Resultado automático depois das correções:**
- 28 pares de contraste passando;
- axe sem violações graves nas 10 telas e painéis cobertos.

## Roteiro manual no aparelho
Faça no iPhone (VoiceOver) e num Android (TalkBack), com o app no Expo Go. Anote abaixo o que não funcionar.

| Verificação | Como | iOS | Android |
|---|---|---|---|
| Ordem de leitura | Deslizar da esquerda para a direita em cada tela: a ordem segue a visual (cabeçalho, busca, categorias, lista, abas) | ☐ | ☐ |
| Nome dos botões | Cada botão é lido com o nome e "botão"; o ☰ é "Abrir menu", o avatar é "Meu perfil", as abas dizem "selecionado" | ☐ | ☐ |
| Formulário de avaliação | As estrelas são lidas como "4 estrelas, selecionado"; cada característica diz a opção marcada | ☐ | ☐ |
| Erros | Enviar o login vazio: os erros são anunciados sem precisar procurar | ☐ | ☐ |
| Menu e filtros | Ao abrir, o foco vai para o painel e não "vaza" para a tela de trás; "Fechar menu" fecha | ☐ | ☐ |
| Mapa | O mapa do local é anunciado com o nome e o endereço; "Abrir no mapa" é lido como link | ☐ | ☐ |
| Fonte grande | Ajustes > Acessibilidade > Tamanho do texto no máximo: nada é cortado nem fica por cima de outra coisa em início, local, formulário e perfil | ☐ | ☐ |
| Contraste alto / modo escuro | O app continua legível com "Aumentar contraste" ligado | ☐ | ☐ |

## Como manter
- **Cor nova:** só pelo `theme.ts`. Ao criar uma combinação nova de texto e fundo, acrescente o par em `__tests__/a11y/contraste.test.ts`.
- **Tela nova:** acrescente uma chamada `audit(page, '...')` em `e2e/a11y.spec.ts`.
- **Botão:** precisa de `accessibilityRole` e de nome. Se tiver texto visível, o nome começa por ele ou o contém (2.5.3).
- **`radio`, `checkbox`, aba ou chip selecionável:** além do `accessibilityState`, use `aria-checked`, `aria-selected` ou `aria-pressed`, porque o react-native-web não repassa o estado para a web.
