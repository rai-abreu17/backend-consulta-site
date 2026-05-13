-- V2__catalog.sql
-- Sprint 003 — Catálogo: Categorias, Consultas e Rituais
-- Conforme SPEC §4.3

CREATE TABLE category (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  slug         VARCHAR(80)  NOT NULL UNIQUE,
  name         VARCHAR(120) NOT NULL,
  description  TEXT,
  display_order SMALLINT NOT NULL DEFAULT 0,
  is_published BOOLEAN  NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_category_updated BEFORE UPDATE ON category
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE service (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  category_id     UUID NOT NULL REFERENCES category(id) ON DELETE RESTRICT,
  type            VARCHAR(20)  NOT NULL CHECK (type IN ('CONSULTATION','RITUAL')),
  slug            VARCHAR(120) NOT NULL UNIQUE,
  name            VARCHAR(160) NOT NULL,
  short_description VARCHAR(280),
  long_description TEXT,
  duration_min    SMALLINT NOT NULL CHECK (duration_min > 0 AND duration_min <= 480),
  price_cents     BIGINT   NOT NULL CHECK (price_cents >= 0),
  currency        CHAR(3)  NOT NULL DEFAULT 'BRL',
  is_published    BOOLEAN  NOT NULL DEFAULT FALSE,
  display_order   SMALLINT NOT NULL DEFAULT 0,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_service_category ON service(category_id);
CREATE INDEX ix_service_published ON service(is_published) WHERE is_published = TRUE;
CREATE TRIGGER trg_service_updated BEFORE UPDATE ON service
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE service_modality_link (
  service_id UUID NOT NULL REFERENCES service(id) ON DELETE CASCADE,
  modality   VARCHAR(20) NOT NULL CHECK (modality IN ('ONLINE','IN_PERSON')),
  PRIMARY KEY (service_id, modality)
);

CREATE TABLE service_photo (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  service_id  UUID NOT NULL REFERENCES service(id) ON DELETE CASCADE,
  storage_key VARCHAR(500) NOT NULL,
  alt_text    VARCHAR(280),
  display_order SMALLINT NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_service_photo_service ON service_photo(service_id);
CREATE TRIGGER trg_service_photo_updated BEFORE UPDATE ON service_photo
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Seed mínimo (idempotente) — categorias iniciais
INSERT INTO category (slug, name, display_order, is_published)
VALUES
  ('consultas', 'Consultas', 1, TRUE),
  ('rituais',   'Rituais',   2, TRUE)
ON CONFLICT (slug) DO NOTHING;
