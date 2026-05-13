-- V2_1__apply_catalog_schema.sql
-- Backfill idempotente do catálogo para ambientes onde o schema remoto
-- ainda não recebeu as tabelas de category/service do V2 original.
-- Não referencia auth.users.

CREATE TABLE IF NOT EXISTS category (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  slug           VARCHAR(80)  NOT NULL,
  name           VARCHAR(120) NOT NULL,
  description    TEXT,
  display_order  SMALLINT     NOT NULL DEFAULT 0,
  is_published   BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_category_slug UNIQUE (slug)
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_category_updated'
      AND tgrelid = 'category'::regclass
      AND NOT tgisinternal
  ) THEN
    EXECUTE 'CREATE TRIGGER trg_category_updated
      BEFORE UPDATE ON category
      FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS service (
  id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  category_id        UUID         NOT NULL,
  type               VARCHAR(20)  NOT NULL,
  slug               VARCHAR(120) NOT NULL,
  name               VARCHAR(160) NOT NULL,
  short_description  VARCHAR(280),
  long_description   TEXT,
  duration_min       SMALLINT     NOT NULL,
  price_cents        BIGINT       NOT NULL,
  currency           CHAR(3)      NOT NULL DEFAULT 'BRL',
  is_published       BOOLEAN      NOT NULL DEFAULT FALSE,
  display_order      SMALLINT     NOT NULL DEFAULT 0,
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT fk_service_category
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE RESTRICT,
  CONSTRAINT uq_service_slug UNIQUE (slug),
  CONSTRAINT chk_service_type
    CHECK (type IN ('CONSULTATION', 'RITUAL')),
  CONSTRAINT chk_service_duration
    CHECK (duration_min > 0 AND duration_min <= 480),
  CONSTRAINT chk_service_price_cents
    CHECK (price_cents >= 0)
);

CREATE INDEX IF NOT EXISTS ix_service_category
  ON service(category_id);

CREATE INDEX IF NOT EXISTS ix_service_published
  ON service(is_published)
  WHERE is_published = TRUE;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_service_updated'
      AND tgrelid = 'service'::regclass
      AND NOT tgisinternal
  ) THEN
    EXECUTE 'CREATE TRIGGER trg_service_updated
      BEFORE UPDATE ON service
      FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS service_modality_link (
  service_id  UUID        NOT NULL,
  modality    VARCHAR(20) NOT NULL,
  CONSTRAINT pk_service_modality_link PRIMARY KEY (service_id, modality),
  CONSTRAINT fk_service_modality_link_service
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
  CONSTRAINT chk_service_modality_link_modality
    CHECK (modality IN ('ONLINE', 'IN_PERSON'))
);

CREATE TABLE IF NOT EXISTS service_photo (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  service_id     UUID         NOT NULL,
  storage_key    VARCHAR(500) NOT NULL,
  alt_text       VARCHAR(280),
  display_order  SMALLINT     NOT NULL DEFAULT 0,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT fk_service_photo_service
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_service_photo_service
  ON service_photo(service_id);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_service_photo_updated'
      AND tgrelid = 'service_photo'::regclass
      AND NOT tgisinternal
  ) THEN
    EXECUTE 'CREATE TRIGGER trg_service_photo_updated
      BEFORE UPDATE ON service_photo
      FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
  END IF;
END $$;

INSERT INTO category (slug, name, display_order, is_published)
VALUES
  ('consultas', 'Consultas', 1, TRUE),
  ('rituais',   'Rituais',   2, TRUE)
ON CONFLICT (slug) DO NOTHING;