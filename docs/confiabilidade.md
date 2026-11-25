# Confiabilidade das avaliações

Como o AccessMap decide quanto confiar em cada avaliação. Qualquer pessoa pode avaliar um local, então o app precisa se proteger de contas criadas só para inflar ou derrubar uma nota, sem calar quem avalia de boa-fé.

São quatro camadas, da mais preventiva para a mais manual:

1. **Limites de frequência:** barram o volume antes de ele chegar à nota.
2. **Peso pelo nível da conta:** quanto cada avaliação pesa na média do local.
3. **Sinais de suspeita:** nota destoante de conta nova e denúncias da comunidade.
4. **Moderação pelo ADMIN:** decide o que ficou em dúvida.

Todos os valores abaixo são os atuais e ficam em [`application.yaml`](../AccessMap.Back/src/main/resources/application.yaml), em `accessmap.review` e `accessmap.auth`. Trocar um valor não exige mudar código; os que têm variável de ambiente mudam até sem gerar uma imagem nova.

As regras vieram da #27 e estão nestas classes do módulo `review`: `ReviewTrustCalculator` (nível e peso), `ReviewSuspicionRules` (o que é suspeito), `ReviewReportService` (denúncias), `ReviewModerationService` (fila do ADMIN) e `ReviewRateLimiter` (limites).

## 1. Nível da conta e peso na média

No momento em que a avaliação é publicada, o nível do autor é calculado e **gravado na avaliação** (`author_trust_level`). Vale o **primeiro** nível, na ordem da tabela, cujo critério a conta cumprir:

| Ordem | Nível | Critério | Peso na média | Propriedades |
|---|---|---|---|---|
| 1 | **Restrita** | 2 ou mais avaliações removidas pela moderação nos últimos 90 dias | **0,25** | `trust.restricted-removals`, `trust.moderation-window`, `trust.weight-restricted` |
| 2 | **Nova** | Conta criada há menos de 7 dias | **0,5** | `rate-limit.new-account-period`, `trust.weight-new` |
| 3 | **Confiável** | Conta com 30 dias ou mais, 5 ou mais avaliações publicadas e nenhuma removida pela moderação nos últimos 90 dias | **1,5** | `trust.trusted-min-age`, `trust.trusted-min-published`, `trust.weight-trusted` |
| 4 | **Regular** | Todas as outras | **1,0** | `trust.weight-regular` |

O que **conta como removida pela moderação**:

- o ADMIN removeu a avaliação pela fila ou pelo `DELETE` da avaliação;
- o próprio autor excluiu uma avaliação que estava **oculta em análise**. Sem isso, bastaria excluir e avaliar de novo para zerar as denúncias (#33).

O autor excluir uma avaliação **publicada** não conta.

Outras regras do nível:

- **As necessidades de acessibilidade não entram no cálculo.** São dado sensível e não dizem nada sobre honestidade.
- **O nível fica com a avaliação.** Se a conta sobe de nível depois, as avaliações antigas mantêm o peso de quando foram publicadas. Editar a avaliação não recalcula o nível.
- **Trocar um peso não exige migrar dados,** porque a avaliação guarda o nível, não o peso. Depois de trocar, o ADMIN chama `POST /api/admin/places/recalculate` uma vez para atualizar as notas guardadas.

### Selo público

| Nível | O que aparece no cartão da avaliação |
|---|---|
| Confiável | "✓ Avaliador confiável" |
| Nova | "Avaliado por conta nova" |
| Regular | Nada |
| Restrita | Nada: a API não devolve esse nível, para não expor uma punição |

### Exemplo numérico

Um local com três avaliações publicadas:

| Avaliação | Nível do autor | Nota | Peso | Nota × peso |
|---|---|---|---|---|
| A | Confiável | 5 | 1,5 | 7,5 |
| B | Regular | 4 | 1,0 | 4,0 |
| C | Nova | 1 | 0,5 | 0,5 |
| **Total** | | | **3,0** | **12,0** |

- **Média simples:** (5 + 4 + 1) ÷ 3 = **3,33**.
- **Média ponderada do AccessMap:** 12,0 ÷ 3,0 = **4,0**.

A conta nova de C, criada na véspera, puxa a nota bem menos do que puxaria numa média simples. Se em vez de C fosse uma conta Restrita com nota 1, a média seria (7,5 + 4,0 + 0,25) ÷ 2,75 = **4,27**.

O local mostra a média ponderada (`averageScore`) e o total de avaliações publicadas (`reviewCount`, sem peso). A lista de locais da API é ordenada por essa mesma média (#108). Ela é recalculada do zero a partir das avaliações publicadas sempre que uma avaliação é criada, editada, excluída, ocultada, restaurada ou removida.

## 2. Limites de frequência

### Avaliações

Contam as avaliações **criadas**, de qualquer status, na última hora e no último dia. Ao passar do limite, a API responde 429 "Você publicou muitas avaliações em pouco tempo". O ADMIN não tem limite.

| Conta | Por hora | Por dia | Variáveis |
|---|---|---|---|
| Nova (menos de 7 dias) | 3 | 10 | `REVIEW_LIMIT_NEW_ACCOUNT_PER_HOUR`, `REVIEW_LIMIT_NEW_ACCOUNT_PER_DAY` |
| As demais | 5 | 20 | `REVIEW_LIMIT_PER_HOUR`, `REVIEW_LIMIT_PER_DAY` |

O prazo de "conta nova" (`REVIEW_LIMIT_NEW_ACCOUNT_PERIOD`, 7 dias) é o mesmo usado no nível Nova e nas denúncias.

Além do limite, cada conta tem **uma avaliação por local**. Para mudar de opinião, a pessoa edita a avaliação que já fez.

### Login e cadastro (#107)

Contam as tentativas **que falharam** numa janela de 15 minutos (`AUTH_LIMIT_WINDOW`). Ao passar do limite, a API responde 429 "Muitas tentativas. Tente novamente em N minutos".

| O que conta | Limite | Variável |
|---|---|---|
| Logins errados no mesmo telefone | 5 | `AUTH_LIMIT_LOGIN_PER_PHONE` |
| Logins errados do mesmo IP | 20 | `AUTH_LIMIT_LOGIN_PER_IP` |
| Cadastros recusados por telefone ou e-mail já em uso, do mesmo IP | 10 | `AUTH_LIMIT_REGISTER_PER_IP` |

- Um login certo zera o contador do telefone, mas não o do IP. Quem testa vários telefones acerta um e continuaria testando.
- O limite é conferido **antes** da senha. Quem está bloqueado não consegue continuar testando senhas.
- Telefone e IP são guardados como hash, não em texto.
- O limite de cadastro existe porque a resposta "já em uso" revela quem tem conta: ele impede varrer números em massa.

## 3. Sinais de suspeita

Uma avaliação suspeita entra na **fila de moderação**, com o motivo (`suspicion_reason`).

### Nota destoante (só conta nova)

Uma avaliação é sinalizada como `NOTA_DESTOANTE` quando as três condições valem juntas:

| Condição | Valor | Propriedade |
|---|---|---|
| O autor é de nível **Nova** | — | — |
| O local já tem avaliações publicadas suficientes | 5 ou mais | `suspicion.min-place-reviews` |
| A nota está longe da média atual do local | 3 pontos ou mais | `suspicion.rating-gap` |

A avaliação **continua no ar** e contando na nota, com o peso de conta nova. Ela só entra na fila para o ADMIN olhar. Conta Regular ou Confiável nunca é sinalizada por nota: discordar da média não é motivo de suspeita.

A checagem vale também na **edição**. Uma conta nova que publique uma nota comum e depois edite para uma nota destoante é sinalizada da mesma forma.

### Denúncias

- **Quem pode:** qualquer conta logada, uma vez por avaliação, e nunca a própria avaliação.
- **Motivos:** informação falsa, conteúdo ofensivo, spam ou outro.
- **Quantas ocultam:** **3 denúncias de contas que não são novas** (`suspicion.reports-to-hide`). A avaliação passa para `OCULTA`, sai da nota do local e entra na fila com o motivo `DENUNCIAS`.
- **Por que conta nova não pesa:** a denúncia de conta nova é registrada, mas não conta para ocultar. Senão, bastaria criar três contas para derrubar qualquer avaliação.
- **Depois que o ADMIN restaura:** as denúncias não ocultam a avaliação de novo, porque ela já foi julgada.

O autor de uma avaliação oculta continua vendo ela como **"em análise"**. Ele não pode editá-la nem publicar outra no mesmo local, mas pode excluí-la; nesse caso, a exclusão conta como remoção pela moderação.

## 4. Moderação pelo ADMIN

A fila (`GET /api/admin/reviews`, tela "Moderação" no app) mostra as avaliações com motivo de suspeita que estão publicadas ou ocultas, das mais antigas para as mais novas, com o número de denúncias de cada uma.

| Ação | Quando pode | Efeito na avaliação | Efeito na nota do local | Efeito no autor |
|---|---|---|---|---|
| **Restaurar** | Avaliação oculta | Volta para `PUBLICADA`, sai da fila e **fica protegida**: novas denúncias não ocultam | Volta a contar | Nenhum |
| **Manter** | Avaliação publicada na fila (nota destoante) | Sai da fila. Denúncias posteriores ainda podem ocultá-la, porque o ADMIN julgou a nota, não denúncias | Continua contando | Nenhum |
| **Remover** | Publicada ou oculta | Passa para `REMOVIDA` (soft delete) e some para todos | Deixa de contar | Conta como remoção para o nível **Restrita** |

Só o ADMIN acessa a fila e essas ações. Um usuário comum recebe 403, e no app a opção nem aparece no menu.

## Resumo de todas as propriedades

| Propriedade (`accessmap.…`) | Valor atual | Variável de ambiente |
|---|---|---|
| `review.trust.trusted-min-age` | 30 dias | — |
| `review.trust.trusted-min-published` | 5 | — |
| `review.trust.moderation-window` | 90 dias | — |
| `review.trust.restricted-removals` | 2 | — |
| `review.trust.weight-restricted` | 0,25 | — |
| `review.trust.weight-new` | 0,5 | — |
| `review.trust.weight-regular` | 1,0 | — |
| `review.trust.weight-trusted` | 1,5 | — |
| `review.suspicion.reports-to-hide` | 3 | — |
| `review.suspicion.rating-gap` | 3 | — |
| `review.suspicion.min-place-reviews` | 5 | — |
| `review.rate-limit.per-hour` | 5 | `REVIEW_LIMIT_PER_HOUR` |
| `review.rate-limit.per-day` | 20 | `REVIEW_LIMIT_PER_DAY` |
| `review.rate-limit.new-account-per-hour` | 3 | `REVIEW_LIMIT_NEW_ACCOUNT_PER_HOUR` |
| `review.rate-limit.new-account-per-day` | 10 | `REVIEW_LIMIT_NEW_ACCOUNT_PER_DAY` |
| `review.rate-limit.new-account-period` | 7 dias | `REVIEW_LIMIT_NEW_ACCOUNT_PERIOD` |
| `auth.rate-limit.window` | 15 minutos | `AUTH_LIMIT_WINDOW` |
| `auth.rate-limit.login-per-phone` | 5 | `AUTH_LIMIT_LOGIN_PER_PHONE` |
| `auth.rate-limit.login-per-ip` | 20 | `AUTH_LIMIT_LOGIN_PER_IP` |
| `auth.rate-limit.register-per-ip` | 10 | `AUTH_LIMIT_REGISTER_PER_IP` |

## Onde cada regra é testada

| Regra | Teste de integração (`AccessMap.Back/src/test/.../review` e `.../auth`) |
|---|---|
| Nível, peso e média ponderada | `ReviewTrustIntegrationTest` |
| Nota destoante | `ReviewSuspicionIntegrationTest` |
| Denúncias e ocultação | `ReviewReportIntegrationTest` |
| Fila e ações do ADMIN | `ReviewModerationIntegrationTest` |
| Avaliação oculta vista pelo autor | `ReviewHiddenAuthorIntegrationTest` |
| Limite de avaliações | `ReviewRateLimitIntegrationTest` |
| Limite de login e cadastro | `AuthRateLimitIntegrationTest` |

No app, o `e2e/confiabilidade.spec.ts` cobre o selo, as denúncias que ocultam e a fila do ADMIN.
