# Deploy da API: Render + Neon + Brevo

A API está publicada em **https://accessmap-7aow.onrender.com** (#35).

| Peça | Onde | Plano |
|---|---|---|
| API (Spring Boot) | [Render](https://render.com), serviço Web a partir do `AccessMap.Back/Dockerfile` | Free |
| Banco PostgreSQL 16 | [Neon](https://neon.tech), região AWS US East 1 (N. Virginia) | Free |
| E-mail da recuperação de senha (#109) | [Brevo](https://www.brevo.com), pela API HTTP | Free (300 e-mails/dia) |

A imagem sobe com o perfil `prod` (`application-prod.yaml`). API e banco ficam na mesma região (Virgínia): cada requisição faz várias consultas, e com os dois longe a API ficaria lenta.

**Por que essas escolhas:**
- A Vercel não roda um processo Java sempre ligado. O Railway funcionaria, mas é pago depois do período de teste.
- O Postgres gratuito do Render expira em cerca de 30 dias; o do Neon não expira.
- O e-mail sai pela API HTTP do Brevo, e não por SMTP, porque os planos gratuitos costumam bloquear as portas de SMTP (o Railway bloqueia no Free e no Hobby).

## Limites do plano gratuito

- **A API "dorme" depois de uns 15 minutos sem uso.** A primeira requisição depois disso demora de 30 a 60 segundos, que é o tempo do Spring subir. **Antes de uma apresentação, abra o health check uns minutos antes.**
- O banco do Neon também pausa sem uso, mas volta em menos de 1 segundo.
- 512 MB de memória: o `Dockerfile` já limita a heap a 75% disso (`-XX:MaxRAMPercentage=75`).

## 1. Banco no Neon

1. Crie o projeto com a região **AWS US East 1 (N. Virginia)** e o **Postgres 16**. Deixe desligados os serviços extras (Better Auth, Object storage, Functions…).
2. Em **Connect**, **desligue "Connection pooling"**: as migrações do Flyway precisam da conexão direta. O host não pode ter `-pooler`.
3. O Neon mostra algo como `postgresql://neondb_owner:SENHA@HOST/neondb?sslmode=require&channel_binding=require`. Separe em:
   - `DB_URL` = `jdbc:postgresql://HOST/neondb?sslmode=require`. Troque o começo por `jdbc:postgresql://` e tire o `&channel_binding=require`, que o driver Java não usa.
   - `DB_USERNAME` = `neondb_owner`
   - `DB_PASSWORD` = a senha

As tabelas não se criam à mão: o Flyway roda as migrações quando a API sobe.

## 2. Remetente e chave no Brevo

1. Em **Remetentes, domínio, IPs → Remetentes**, cadastre o e-mail que aparece no "De:" e confirme pelo link que chega nele.
2. Em **SMTP & API → aba "Chaves API e MCP"** (não a aba "SMTP"), gere uma chave:
   - **Acesso: Restrito**, com **E-mails transacionais: Todos** e o resto em "Nenhum". Assim, se a chave vazar, ela só serve para mandar e-mail.
   - A chave certa começa com `xkeysib-`. A que começa com `xsmtpsib-` é de SMTP e é recusada pela API ("Key not found").
3. Em **Segurança → IPs autorizados**, **desative o bloqueio de IPs não autorizados**. O Render não tem IP fixo; com o bloqueio ligado, o envio falha com `unrecognised IP address`.

A chave expira na data escolhida **ou depois de 90 dias sem uso**. Se a recuperação de senha parar e o log mostrar `401 Key not found`, gere outra e troque só a variável.

## 3. Serviço no Render

**New → Web Service**, a partir do repositório do GitHub:

| Campo | Valor |
|---|---|
| Language | **Docker** |
| Branch | `main` |
| Region | **Virginia (US East)**, a mesma do Neon |
| Root Directory | **vazio** |
| Compute | **Free ($0)** |
| Advanced → Docker Build Context Directory | `AccessMap.Back` |
| Advanced → Dockerfile Path | `AccessMap.Back/Dockerfile` |
| Advanced → Health Check Path | `/actuator/health/readiness` |
| Advanced → Build Filters → Included Paths | `AccessMap.Back/**`. Assim, commits só no app não publicam a API de novo |
| Auto-Deploy | On Commit: cada merge na `main` publica |

Os dois caminhos do Docker contam a partir da raiz do repositório, por isso o Root Directory fica vazio. O contexto precisa ser `AccessMap.Back`, porque o `Dockerfile` copia `mvnw`, `pom.xml` e `src/` de lá.

## 4. Variáveis da API

No Render, em **Environment**:

| Variável | Valor | Obrigatória |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://HOST/neondb?sslmode=require` (etapa 1) | Sim |
| `DB_USERNAME` | `neondb_owner` | Sim |
| `DB_PASSWORD` | Senha do Neon | Sim |
| `JWT_SECRET` | Nova, com 32+ caracteres. **Nunca** reaproveite a do `.env` local. Gere com o comando abaixo | Sim |
| `BREVO_API_KEY` | Chave `xkeysib-` (etapa 2) | Sim, para a recuperação de senha. Sem ela a API sobe, mas não envia o código |
| `MAIL_FROM` | Remetente verificado no Brevo | Sim, junto com a chave |
| `MAIL_FROM_NAME` | Nome no "De:" | Não: o padrão é `AccessMap` |
| `ADMIN_PHONE` e `ADMIN_PASSWORD` | Telefone (só números, com DDD) e senha do primeiro ADMIN | Só na primeira subida |
| `CORS_ALLOWED_ORIGINS` | Endereço do app web publicado, se houver | Não: o app no celular não precisa |

`PORT` não precisa: o Render define (10000) e a API já lê essa variável. `FORWARD_HEADERS_STRATEGY` já vem `native` no perfil `prod`, para o limite de login por IP (#107) ver o IP real de quem acessa, e não o do proxy do Render.

Gerar o `JWT_SECRET` no PowerShell (copia para a área de transferência sem mostrar na tela):

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b) | Set-Clipboard
```

Os limites de avaliação (`REVIEW_LIMIT_*`) e de login (`AUTH_LIMIT_*`) são opcionais, com os mesmos padrões do `.env.example`.

## 5. Depois da primeira subida

1. Abra `https://<domínio>/actuator/health/readiness` e confira se responde `{"status":"UP"}`. Depois abra `https://<domínio>/swagger-ui.html`.
2. Entre com o ADMIN pelo Swagger (`POST /api/auth/login`) ou pelo app.
3. **Apague `ADMIN_PHONE` e `ADMIN_PASSWORD`** das variáveis e salve. O ADMIN já existe no banco, e a senha não precisa ficar guardada no Render.
4. Se o banco já tinha avaliações de antes da nota ponderada (#108), chame `POST /api/admin/places/recalculate` uma vez, como ADMIN.

## 6. Health check

| Caminho | Responde `UP` quando | Uso |
|---|---|---|
| `/actuator/health/readiness` | A API subiu **e** o banco responde | Health check do Render: ele só manda tráfego para a versão nova depois disso, e mantém a anterior no ar se a nova não subir |
| `/actuator/health/liveness` | O processo está vivo (não olha o banco) | `HEALTHCHECK` do `Dockerfile` |
| `/actuator/health` | Os dois acima | Conferir na mão |

Sem login, a resposta é só `{"status":"UP"}` (ou `DOWN`, com 503). Com o token de ADMIN aparecem os detalhes. Os outros endpoints do Actuator (`env`, `metrics`, `beans`…) não ficam expostos.

Os logs ficam em `INFO`, sem SQL e sem o código de recuperação de senha. Para investigar algo, defina `LOG_LEVEL_APP=DEBUG` e volte para `INFO` depois.

## 7. Apontar o app para a API

No `AccessMap.Front/.env.local`:

```
EXPO_PUBLIC_API_URL=https://accessmap-7aow.onrender.com
```

Deixe **uma só** linha `EXPO_PUBLIC_API_URL` ativa e reinicie o `npm start`. Com a API publicada, o celular funciona em qualquer rede, sem depender do computador ligado.

## Erros comuns

| Sintoma ou mensagem no log | Causa |
|---|---|
| `'url' must start with "jdbc"` | `DB_URL` sem o `jdbc:` na frente, ou copiada direto do Neon |
| `password authentication failed` | `DB_USERNAME` ou `DB_PASSWORD` errados |
| `SSL` / `connection refused` no banco | Falta `?sslmode=require` na `DB_URL` |
| Flyway trava ou falha nas migrações | Host com `-pooler`: desligue o "Connection pooling" no Neon |
| `Defina a variavel de ambiente JWT_SECRET` | Falta `JWT_SECRET`, ou tem menos de 32 caracteres |
| Build falha com "Dockerfile not found" ou sem `mvnw` | Context Directory e Dockerfile Path diferentes da tabela da etapa 3 |
| Deploy fica em "Waiting for health check" e falha | A API não conectou no banco: veja as linhas de erro acima no log |
| `Falha ao enviar o código` + `Key not found` | Chave SMTP (`xsmtpsib-`) no lugar da chave da API, ou chave expirada |
| `Falha ao enviar o código` + `unrecognised IP address` | Bloqueio de IP ligado no Brevo (etapa 2, item 3) |
| `BREVO_API_KEY não configurada` | Variável ausente ou com outro nome |
| Primeira requisição demora quase 1 minuto | Normal no plano gratuito: a API estava dormindo |
