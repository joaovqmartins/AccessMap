# Deploy da API no Railway

A API roda no Railway a partir do `AccessMap.Back/Dockerfile`, com o perfil `prod` (`application-prod.yaml`) e um banco PostgreSQL do próprio Railway. Este guia é o passo a passo da #35.

## 1. Projeto e banco

1. No Railway, crie um projeto a partir do repositório do GitHub.
2. Adicione um banco **PostgreSQL** ao projeto. O serviço do banco se chama `Postgres` por padrão. Se tiver outro nome, troque `Postgres` nas variáveis abaixo.
3. Nas configurações do serviço da API:
   - **Root Directory:** `AccessMap.Back`. O repositório tem duas pastas, e a API está nesta. O Railway encontra o `Dockerfile` dela e o usa sozinho.
   - **Networking:** gere um domínio público, apontando para a porta `8080`.

Não usamos `railway.toml`: o "Config as Code" do Railway foi descontinuado e só vale para serviços antigos até 01/12/2026. A configuração fica no painel.

## 2. Variáveis da API

| Variável | Valor | Obrigatória |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` | Sim |
| `DB_USERNAME` | `${{Postgres.PGUSER}}` | Sim |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` | Sim |
| `JWT_SECRET` | Gere uma nova com `openssl rand -base64 48`. **Nunca** reaproveite a do `.env` local | Sim |
| `PORT` | `8080` | Recomendada: a API escuta nela, e o domínio aponta para ela |
| `ADMIN_PHONE` e `ADMIN_PASSWORD` | Telefone e senha do primeiro ADMIN | Só na primeira subida |
| `CORS_ALLOWED_ORIGINS` | Endereço do app web publicado, se houver | Não: o app no celular não precisa |
| `FORWARD_HEADERS_STRATEGY` | Já vem `native` no perfil `prod` | Não |

O `DB_URL` não pode usar o `DATABASE_URL` do Railway direto: ele vem no formato `postgresql://usuario:senha@host/banco`, e o Spring só lê endereços que começam com `jdbc:`. Por isso a URL é montada com as variáveis `PG*`.

Os limites de avaliação (`REVIEW_LIMIT_*`) e de login (`AUTH_LIMIT_*`) são opcionais, com os mesmos padrões do `.env.example`.

## 3. Depois da primeira subida

1. Abra `https://<domínio>/actuator/health/readiness` e confira se responde `{"status":"UP"}`. Depois abra `https://<domínio>/swagger-ui.html`.
2. Entre com o ADMIN pelo app ou pelo Swagger (`/api/auth/login`).
3. Remova `ADMIN_PHONE` e `ADMIN_PASSWORD` das variáveis: o ADMIN já existe, e a senha não precisa ficar guardada lá.
4. Se o banco já tinha avaliações de antes da nota ponderada (#108), chame `POST /api/admin/places/recalculate` uma vez, como ADMIN.
5. Confira o limite de login por IP (#107): duas pessoas em redes diferentes, errando a senha, precisam contar separadas.

## 4. Health check

Em **Settings → Deploy → Healthcheck Path**, coloque `/actuator/health/readiness`. Assim o Railway só manda tráfego para a versão nova depois que ela conectou no banco e o Flyway terminou, e mantém a anterior no ar se a nova não subir.

| Caminho | Responde `UP` quando | Uso |
|---|---|---|
| `/actuator/health/readiness` | A API subiu **e** o banco responde | Healthcheck do Railway |
| `/actuator/health/liveness` | O processo está vivo (não olha o banco) | `HEALTHCHECK` do `Dockerfile` |
| `/actuator/health` | Os dois acima | Conferir na mão |

Sem login, a resposta é só `{"status":"UP"}` (ou `DOWN`, com 503). Com o token de ADMIN aparecem os detalhes, como o estado do banco. Os outros endpoints do Actuator (`env`, `metrics`, `beans`…) não ficam expostos, nem para o ADMIN.

Os logs ficam em `INFO`, sem SQL. Para investigar algo, defina `LOG_LEVEL_APP=DEBUG` nas variáveis e volte para `INFO` depois.

## 5. Apontar o app para a API

No `AccessMap.Front/.env.local`, troque `EXPO_PUBLIC_API_URL` pelo domínio do Railway, com `https://`, e reinicie o `npm start`.

## Erros comuns ao subir

| Mensagem no log | Causa |
|---|---|
| `'url' must start with "jdbc"` | Falta `DB_URL`, ou foi usado o `DATABASE_URL` do Railway em vez da URL `jdbc:` |
| `password authentication failed` | `DB_USERNAME` ou `DB_PASSWORD` errados ou ausentes |
| `Defina a variavel de ambiente JWT_SECRET` | Falta `JWT_SECRET`, ou ela tem menos de 32 caracteres |
| A API sobe, mas o domínio dá erro 502 | O domínio aponta para outra porta: confira `PORT` e a porta do domínio |
| Deploy fica em "Healthcheck failed" | A API não conectou no banco a tempo, ou o caminho está errado: confira o log e o `Healthcheck Path` |
