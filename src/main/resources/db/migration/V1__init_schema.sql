-- V1__init_schema.sql
-- Descrição: Cria extensões obrigatórias, função de trigger set_updated_at(),
--            tabelas admin_user e refresh_token (módulo de autenticação).
-- Dependências: Nenhuma (migration inicial).
-- Impacto em dados: Nenhum — criação de schema em banco limpo.

-- ═══════════════════════════════════════════════════════════════
-- 1. Extensões obrigatórias
-- ═══════════════════════════════════════════════════════════════
CREATE EXTENSION IF NOT EXISTS pgcrypto;    -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS btree_gist;  -- EXCLUDE USING gist (anti-sobreposição de bookings)

-- ═══════════════════════════════════════════════════════════════
-- 2. Função reutilizável para atualização automática de updated_at
--    Chamada por triggers em todas as tabelas do sistema.
-- ═══════════════════════════════════════════════════════════════
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

-- ═══════════════════════════════════════════════════════════════
-- 3. Tabela admin_user — Administradores do Terreiro
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE admin_user (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  email           VARCHAR(254) NOT NULL UNIQUE,
  password_hash   VARCHAR(72)  NOT NULL,
  display_name    VARCHAR(120) NOT NULL,
  role            VARCHAR(20)  NOT NULL DEFAULT 'ADMIN'
                  CHECK (role IN ('ADMIN', 'SUPER_ADMIN')),
  is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
  failed_attempts SMALLINT     NOT NULL DEFAULT 0,
  locked_until    TIMESTAMPTZ,
  last_login_at   TIMESTAMPTZ,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_admin_user_updated
  BEFORE UPDATE ON admin_user
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ═══════════════════════════════════════════════════════════════
-- 4. Tabela refresh_token — Tokens de atualização de sessão
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE refresh_token (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_user_id   UUID         NOT NULL REFERENCES admin_user(id) ON DELETE CASCADE,
  token_hash      CHAR(64)     NOT NULL UNIQUE,
  expires_at      TIMESTAMPTZ  NOT NULL,
  revoked_at      TIMESTAMPTZ,
  user_agent      VARCHAR(500),
  ip              INET,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Índice para busca de tokens ativos por administrador (FK + filtro parcial)
CREATE INDEX ix_refresh_token_user_active
  ON refresh_token(admin_user_id)
  WHERE revoked_at IS NULL;

CREATE TRIGGER trg_refresh_token_updated
  BEFORE UPDATE ON refresh_token
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ═══════════════════════════════════════════════════════════════
-- 5. Tabela customer — Consulentes (PII criptografada)
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE customer (
  id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

  -- PII criptografada na aplicação (AES-GCM-256 via Google Tink).
  -- Colunas armazenam ciphertext em Base64, prefixado com versão da chave (kv:1:).
  full_name_enc      TEXT         NOT NULL,
  email_enc          TEXT         NOT NULL,
  phone_enc          TEXT         NOT NULL,

  -- Hash determinístico HMAC-SHA256(salt + lower(email)) para busca sem decifrar.
  email_lookup_hash  CHAR(64)     NOT NULL UNIQUE,

  -- Controle LGPD — anonimização de dados pessoais
  is_anonymized      BOOLEAN      NOT NULL DEFAULT FALSE,
  anonymized_at      TIMESTAMPTZ,

  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_customer_updated
  BEFORE UPDATE ON customer
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
