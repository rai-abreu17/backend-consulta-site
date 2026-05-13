# Backend Consulta Site

Backend Spring Boot isolado do projeto Consulta Site, preparado para deploy no Railway.

## Stack

- Java 21
- Spring Boot 3.3.5
- Maven
- PostgreSQL
- Redis (Redisson)

## Deploy no Railway

O repositório já inclui `railway.toml` com:

- build: `mvn clean package -DskipTests`
- start: `java -Dserver.port=$PORT -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} $JAVA_OPTS -jar target/*jar`
- healthcheck: `/v3/api-docs`

## Variáveis obrigatórias

Configure estas variáveis no serviço do Railway:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `REDIS_URL`
- `JWT_SECRET`
- `PII_SECRET_KEY`
- `PII_HASH_SALT`
- `MP_ACCESS_TOKEN`
- `MP_WEBHOOK_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`
- `SPRING_FLYWAY_BASELINE_VERSION=11`

## Banco de dados

O backend pode subir com um Postgres do Railway ou com o banco remoto ja existente no Supabase. O importante e preencher `DB_URL`, `DB_USER` e `DB_PASSWORD` com um conjunto valido.

### Opcao 1: Railway Postgres

No backend, use referencias de variaveis do proprio Railway:

- `DB_URL=${{Postgres.DATABASE_URL}}`
- `DB_USER=${{Postgres.PGUSER}}`
- `DB_PASSWORD=${{Postgres.PGPASSWORD}}`

### Opcao 2: Supabase existente

Se quiser manter o banco remoto que ja foi validado fora do Railway, use o JDBC do Supabase como `DB_URL` e informe `DB_USER`/`DB_PASSWORD` correspondentes.

## Redis

Em producao o servico usa Redisson para locks distribuidos. Por isso, o backend precisa de um Redis acessivel e a variavel abaixo configurada:

- `REDIS_URL=${{Redis.REDIS_URL}}`

## Observacoes de deploy

- O container agora assume `SPRING_PROFILES_ACTIVE=prod` por padrao para evitar subir com `application-local.yml` no Railway.
- Nao e necessario criar a variavel `PORT` manualmente; o Railway injeta esse valor automaticamente.
- Se for usar o mesmo banco Supabase existente, mantenha o baseline do Flyway em `11`.

## Variáveis opcionais

- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_SMTP_AUTH`
- `MAIL_SMTP_STARTTLS`
- `MAIL_FROM`
- `JWT_EXPIRATION`
- `TERREIRO_ENDERECO`
- `TERREIRO_MAPS_URL`
- `TERREIRO_LINK_ONLINE`

## Observações

- Este backend já foi validado localmente contra o banco remoto Supabase do projeto.
- O serviço precisa de Redis em produção porque usa Redisson para locks distribuídos.