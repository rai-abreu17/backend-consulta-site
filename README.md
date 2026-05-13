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
- start: `java -Dserver.port=$PORT $JAVA_OPTS -jar target/*jar`
- healthcheck: `/v3/api-docs`

## Variáveis obrigatórias

Configure estas variáveis no serviço do Railway:

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
- Se for usar o mesmo banco Supabase existente, mantenha o baseline do Flyway em `11`.
- O serviço precisa de Redis em produção porque usa Redisson para locks distribuídos.