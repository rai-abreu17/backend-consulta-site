-- V7__seed_and_indexes.sql
-- Índices finais de performance + seeds mínimos para bootstrap do MVP.
-- Pré-requisitos:
--   - V1/V2_1/V3/V4/V5/V6 já aplicadas
--   - função set_updated_at() já existente
-- Observação operacional:
--   - a senha seeded abaixo corresponde a "senha123" com BCrypt
--   - após o primeiro login, a senha deve ser trocada via aplicação

-- ============================================================
-- 1. Índice para filtros administrativos por status + início
--    Compatível com TIMESTAMPTZ no Postgres/Supabase
-- ============================================================
CREATE INDEX IF NOT EXISTS ix_booking_status_start_at
  ON booking(status, start_at);

-- ============================================================
-- 2. Seed do administrador inicial
-- ============================================================
INSERT INTO admin_user (
  email,
  password_hash,
  display_name,
  role,
  is_active,
  failed_attempts,
  created_at,
  updated_at
)
VALUES (
  'admin@terreiro.app',
  '$2a$10$ktkRYGRJtuGAyiAgANMqrOh5ybQDp5Rb4FpqrP3BCdOQB/8EtKwy.',
  'Administrador Inicial',
  'SUPER_ADMIN',
  TRUE,
  0,
  now(),
  now()
)
ON CONFLICT (email) DO NOTHING;

-- ============================================================
-- 3. Seed base de páginas institucionais
--    Conforme exemplos da Spec, começam despublicadas
-- ============================================================
INSERT INTO content_page (
  slug,
  title,
  body_md,
  is_published,
  published_at,
  created_at,
  updated_at
)
VALUES
(
  'sobre-o-terreiro',
  'Sobre o Terreiro',
  '# Nosso terreiro

[Texto a ser preenchido pela liderança religiosa]
',
  FALSE,
  NULL,
  now(),
  now()
),
(
  'sobre-rei-sebastiao',
  'Sobre Rei Sebastião',
  '# Rei Sebastião na Encantaria Maranhense

[Texto a ser preenchido]
',
  FALSE,
  NULL,
  now(),
  now()
),
(
  'como-funcionam-as-consultas',
  'Como funcionam as consultas',
  '# Tiragens e jogos

[Texto a ser preenchido]
',
  FALSE,
  NULL,
  now(),
  now()
)
ON CONFLICT (slug) DO NOTHING;