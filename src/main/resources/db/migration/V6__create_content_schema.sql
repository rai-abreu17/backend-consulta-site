-- V6__create_content_schema.sql
-- Schema de páginas institucionais em Markdown.
-- Não referencia auth.users.

CREATE TABLE IF NOT EXISTS content_page (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  slug              VARCHAR(120) NOT NULL,
  title             VARCHAR(200) NOT NULL,
  body_md           TEXT         NOT NULL,
  hero_image_key    VARCHAR(500),
  meta_description  VARCHAR(280),
  is_published      BOOLEAN      NOT NULL DEFAULT FALSE,
  published_at      TIMESTAMPTZ,
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_content_page_slug UNIQUE (slug)
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_content_page_updated'
      AND tgrelid = 'content_page'::regclass
      AND NOT tgisinternal
  ) THEN
    EXECUTE 'CREATE TRIGGER trg_content_page_updated
      BEFORE UPDATE ON content_page
      FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
  END IF;
END $$;