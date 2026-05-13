-- V10__seed_demo_data.sql
-- Dados de demonstracao para evitar telas vazias em ambiente de teste.
-- Pressupoe V9 aplicada para que booking.view_token exista.

-- ============================================================
-- 1. Publica e atualiza paginas institucionais basicas
-- ============================================================
INSERT INTO content_page (
  slug,
  title,
  body_md,
  meta_description,
  is_published,
  published_at,
  created_at,
  updated_at
)
VALUES
(
  'sobre-o-terreiro',
  'Sobre o Terreiro',
  E'# Nosso terreiro\n\nEspaco de acolhimento espiritual com atendimento orientado, escuta cuidadosa e rituais tradicionais de amparo.',
  'Conheca a historia e a proposta espiritual do terreiro.',
  TRUE,
  now(),
  now(),
  now()
),
(
  'sobre-rei-sebastiao',
  'Sobre Rei Sebastiao',
  E'# Rei Sebastiao\n\nTexto demonstrativo para validacao da pagina publica sobre a linha espiritual e seus fundamentos.',
  'Pagina institucional sobre Rei Sebastiao para testes do portal.',
  TRUE,
  now(),
  now(),
  now()
),
(
  'como-funcionam-as-consultas',
  'Como funcionam as consultas',
  E'# Como funcionam as consultas\n\nEscolha o servico, selecione um horario disponivel e finalize o agendamento para prosseguir com o atendimento.',
  'Explicacao simplificada do fluxo de consultas para ambiente de testes.',
  TRUE,
  now(),
  now(),
  now()
)
ON CONFLICT (slug) DO UPDATE
SET title = EXCLUDED.title,
    body_md = EXCLUDED.body_md,
    meta_description = EXCLUDED.meta_description,
    is_published = EXCLUDED.is_published,
    published_at = COALESCE(content_page.published_at, EXCLUDED.published_at),
    updated_at = now();

-- ============================================================
-- 2. Servicos publicados para o catalogo publico e admin
-- ============================================================
INSERT INTO service (
  category_id,
  type,
  slug,
  name,
  short_description,
  long_description,
  duration_min,
  price_cents,
  currency,
  is_published,
  display_order,
  created_at,
  updated_at
)
SELECT
  c.id,
  'CONSULTATION',
  'consulta-buzios-demo',
  'Consulta de Buzios',
  'Leitura espiritual com orientacoes iniciais para o consulente.',
  E'Atendimento demonstrativo para testes do fluxo publico, exibido no catalogo e apto para geracao de slots.',
  60,
  18000,
  'BRL',
  TRUE,
  1,
  now(),
  now()
FROM category c
WHERE c.slug = 'consultas'
ON CONFLICT (slug) DO UPDATE
SET category_id = EXCLUDED.category_id,
    type = EXCLUDED.type,
    name = EXCLUDED.name,
    short_description = EXCLUDED.short_description,
    long_description = EXCLUDED.long_description,
    duration_min = EXCLUDED.duration_min,
    price_cents = EXCLUDED.price_cents,
    currency = EXCLUDED.currency,
    is_published = EXCLUDED.is_published,
    display_order = EXCLUDED.display_order,
    updated_at = now();

INSERT INTO service (
  category_id,
  type,
  slug,
  name,
  short_description,
  long_description,
  duration_min,
  price_cents,
  currency,
  is_published,
  display_order,
  created_at,
  updated_at
)
SELECT
  c.id,
  'RITUAL',
  'ritual-limpeza-demo',
  'Ritual de Limpeza Espiritual',
  'Ritual demonstrativo para atendimentos presenciais.',
  E'Ritual de exemplo usado para validar listagens administrativas, detalhes de servico e integracao com agendamento.',
  90,
  32000,
  'BRL',
  TRUE,
  1,
  now(),
  now()
FROM category c
WHERE c.slug = 'rituais'
ON CONFLICT (slug) DO UPDATE
SET category_id = EXCLUDED.category_id,
    type = EXCLUDED.type,
    name = EXCLUDED.name,
    short_description = EXCLUDED.short_description,
    long_description = EXCLUDED.long_description,
    duration_min = EXCLUDED.duration_min,
    price_cents = EXCLUDED.price_cents,
    currency = EXCLUDED.currency,
    is_published = EXCLUDED.is_published,
    display_order = EXCLUDED.display_order,
    updated_at = now();

INSERT INTO service_modality_link (service_id, modality)
SELECT s.id, modalidade.modality
FROM service s
JOIN (VALUES ('ONLINE'), ('IN_PERSON')) AS modalidade(modality) ON TRUE
WHERE s.slug = 'consulta-buzios-demo'
ON CONFLICT (service_id, modality) DO NOTHING;

INSERT INTO service_modality_link (service_id, modality)
SELECT s.id, 'IN_PERSON'
FROM service s
WHERE s.slug = 'ritual-limpeza-demo'
ON CONFLICT (service_id, modality) DO NOTHING;

-- ============================================================
-- 3. Disponibilidade recorrente e uma excecao de exemplo
-- ============================================================
INSERT INTO availability_rule (
  id,
  weekday,
  start_time,
  end_time,
  modalities,
  is_active,
  created_at,
  updated_at
)
VALUES
('00000000-0000-0000-0000-000000000101', 0, '09:00', '17:00', 'ONLINE,IN_PERSON', TRUE, now(), now()),
('00000000-0000-0000-0000-000000000102', 1, '09:00', '17:00', 'ONLINE,IN_PERSON', TRUE, now(), now()),
('00000000-0000-0000-0000-000000000103', 2, '09:00', '17:00', 'ONLINE,IN_PERSON', TRUE, now(), now()),
('00000000-0000-0000-0000-000000000104', 3, '09:00', '17:00', 'ONLINE,IN_PERSON', TRUE, now(), now()),
('00000000-0000-0000-0000-000000000105', 4, '09:00', '17:00', 'ONLINE,IN_PERSON', TRUE, now(), now()),
('00000000-0000-0000-0000-000000000106', 5, '09:00', '17:00', 'ONLINE,IN_PERSON', TRUE, now(), now()),
('00000000-0000-0000-0000-000000000107', 6, '09:00', '17:00', 'ONLINE,IN_PERSON', TRUE, now(), now())
ON CONFLICT (id) DO UPDATE
SET weekday = EXCLUDED.weekday,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    modalities = EXCLUDED.modalities,
    is_active = EXCLUDED.is_active,
    updated_at = now();

INSERT INTO day_override (
  id,
  date,
  is_closed,
  start_time,
  end_time,
  modalities,
  reason,
  created_at,
  updated_at
)
VALUES (
  '00000000-0000-0000-0000-000000000201',
  CURRENT_DATE + 2,
  FALSE,
  '18:00',
  '21:00',
  'ONLINE',
  'Plantao especial de teste',
  now(),
  now()
)
ON CONFLICT (date) DO UPDATE
SET is_closed = EXCLUDED.is_closed,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    modalities = EXCLUDED.modalities,
    reason = EXCLUDED.reason,
    updated_at = now();

-- ============================================================
-- 4. Consulentes de demonstracao com PII criptografada
--    Cifrados usando a chave dev padrao do projeto.
-- ============================================================
INSERT INTO customer (
  full_name_enc,
  email_enc,
  phone_enc,
  email_lookup_hash,
  is_anonymized,
  created_at,
  updated_at
)
VALUES
(
  'kv:1:YHfZ0/LOcOM7qi3rVj8lNHKdcAQsd0p6zIG6UF2suSuJTfwogVi2IO8FLw==',
  'kv:1:FnJoWqh2RvOYNy09Z+vvIiRP+3CaAew1ZrArcos2qVmxBm+n/PWYwaYk4N53QmZdG1Qo9w==',
  'kv:1:j11dxV/+jCzfANNNTu/2SvShStxDw7HttWQlA2RzaCaorshzU8BhOBRp',
  'ab2b46f09241541692215125016f1b7702b094cef1805305f1454bec80e74d12',
  FALSE,
  now(),
  now()
),
(
  'kv:1:X9j4dZdLCaaBw5m5nYpKcgyWSa5tmn1O/bOYIobJscsEbFC/GMPY5w==',
  'kv:1:akFwY8DA5IXo4VkgLZphkrllCjLb0DyGR/Ge7PXn0srZsNrptamUEiEV0g1bjHAxFaED',
  'kv:1:CzJ+JkHGoOWGRtq4W3AN2HsYI709wdbuDF34pauDEPab8XmX/NdGKyAA',
  '8b49b899f2ac522892bb5a3cca464462902ed197dd024352ab53c5cc3e108e63',
  FALSE,
  now(),
  now()
)
ON CONFLICT (email_lookup_hash) DO UPDATE
SET full_name_enc = EXCLUDED.full_name_enc,
    email_enc = EXCLUDED.email_enc,
    phone_enc = EXCLUDED.phone_enc,
    is_anonymized = FALSE,
    anonymized_at = NULL,
    updated_at = now();

-- ============================================================
-- 5. Bookings e pagamentos de exemplo para o dashboard admin
-- ============================================================
INSERT INTO booking (
  id,
  service_id,
  customer_id,
  modality,
  start_at,
  end_at,
  status,
  hold_expires_at,
  notes_admin,
  price_snapshot_cents,
  currency,
  created_at,
  updated_at
)
SELECT
  '00000000-0000-0000-0000-000000000301',
  s.id,
  c.id,
  'CONFIRMED',
  ((CURRENT_DATE + 1) + TIME '14:00') AT TIME ZONE 'America/Fortaleza',
  ((CURRENT_DATE + 1) + TIME '15:00') AT TIME ZONE 'America/Fortaleza',
  'CONFIRMED',
  NULL,
  'Agendamento demonstrativo confirmado.',
  18000,
  'BRL',
  now(),
  now()
FROM service s
JOIN customer c
  ON c.email_lookup_hash = 'ab2b46f09241541692215125016f1b7702b094cef1805305f1454bec80e74d12'
WHERE s.slug = 'consulta-buzios-demo'
ON CONFLICT (id) DO UPDATE
SET service_id = EXCLUDED.service_id,
    customer_id = EXCLUDED.customer_id,
    modality = EXCLUDED.modality,
    start_at = EXCLUDED.start_at,
    end_at = EXCLUDED.end_at,
    status = EXCLUDED.status,
    hold_expires_at = EXCLUDED.hold_expires_at,
    notes_admin = EXCLUDED.notes_admin,
    price_snapshot_cents = EXCLUDED.price_snapshot_cents,
    currency = EXCLUDED.currency,
    updated_at = now();

INSERT INTO booking (
  id,
  service_id,
  customer_id,
  modality,
  start_at,
  end_at,
  status,
  hold_expires_at,
  notes_admin,
  price_snapshot_cents,
  currency,
  created_at,
  updated_at
)
SELECT
  '00000000-0000-0000-0000-000000000302',
  s.id,
  c.id,
  'IN_PERSON',
  ((CURRENT_DATE + 2) + TIME '10:00') AT TIME ZONE 'America/Fortaleza',
  ((CURRENT_DATE + 2) + TIME '11:30') AT TIME ZONE 'America/Fortaleza',
  'PENDING_PAYMENT',
  now() + INTERVAL '2 days',
  'Reserva demonstrativa aguardando pagamento.',
  32000,
  'BRL',
  now(),
  now()
FROM service s
JOIN customer c
  ON c.email_lookup_hash = '8b49b899f2ac522892bb5a3cca464462902ed197dd024352ab53c5cc3e108e63'
WHERE s.slug = 'ritual-limpeza-demo'
ON CONFLICT (id) DO UPDATE
SET service_id = EXCLUDED.service_id,
    customer_id = EXCLUDED.customer_id,
    modality = EXCLUDED.modality,
    start_at = EXCLUDED.start_at,
    end_at = EXCLUDED.end_at,
    status = EXCLUDED.status,
    hold_expires_at = EXCLUDED.hold_expires_at,
    notes_admin = EXCLUDED.notes_admin,
    price_snapshot_cents = EXCLUDED.price_snapshot_cents,
    currency = EXCLUDED.currency,
    updated_at = now();

INSERT INTO payment (
  booking_id,
  provider,
  provider_pref_id,
  provider_payment_id,
  status,
  amount_cents,
  currency,
  init_point_url,
  raw_response,
  approved_at,
  created_at,
  updated_at
)
SELECT
  b.id,
  'MERCADO_PAGO',
  'pref_demo_conf_001',
  'pay_demo_conf_001',
  'APPROVED',
  18000,
  'BRL',
  'https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref_demo_conf_001',
  '{"source":"seed","status":"approved"}'::jsonb,
  now(),
  now(),
  now()
FROM booking b
WHERE b.id = '00000000-0000-0000-0000-000000000301'
ON CONFLICT (booking_id) DO UPDATE
SET provider = EXCLUDED.provider,
    provider_pref_id = EXCLUDED.provider_pref_id,
    provider_payment_id = EXCLUDED.provider_payment_id,
    status = EXCLUDED.status,
    amount_cents = EXCLUDED.amount_cents,
    currency = EXCLUDED.currency,
    init_point_url = EXCLUDED.init_point_url,
    raw_response = EXCLUDED.raw_response,
    approved_at = EXCLUDED.approved_at,
    updated_at = now();

INSERT INTO payment (
  booking_id,
  provider,
  provider_pref_id,
  provider_payment_id,
  status,
  amount_cents,
  currency,
  init_point_url,
  raw_response,
  approved_at,
  created_at,
  updated_at
)
SELECT
  b.id,
  'MERCADO_PAGO',
  'pref_demo_pending_001',
  NULL,
  'PENDING',
  32000,
  'BRL',
  'https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref_demo_pending_001',
  '{"source":"seed","status":"pending"}'::jsonb,
  NULL,
  now(),
  now()
FROM booking b
WHERE b.id = '00000000-0000-0000-0000-000000000302'
ON CONFLICT (booking_id) DO UPDATE
SET provider = EXCLUDED.provider,
    provider_pref_id = EXCLUDED.provider_pref_id,
    provider_payment_id = EXCLUDED.provider_payment_id,
    status = EXCLUDED.status,
    amount_cents = EXCLUDED.amount_cents,
    currency = EXCLUDED.currency,
    init_point_url = EXCLUDED.init_point_url,
    raw_response = EXCLUDED.raw_response,
    approved_at = EXCLUDED.approved_at,
    updated_at = now();