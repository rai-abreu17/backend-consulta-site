-- V11__seed_email_outbox_demo.sql
-- Outbox de e-mail demonstrativo para testes administrativos.

INSERT INTO email_outbox_event (
  booking_id,
  event_type,
  status,
  attempts,
  created_at,
  processed_at
)
SELECT
  b.id,
  'BOOKING_CONFIRMED',
  'PENDING',
  0,
  now(),
  NULL
FROM booking b
WHERE b.id = '00000000-0000-0000-0000-000000000301'
ON CONFLICT (booking_id, event_type) DO UPDATE
SET status = EXCLUDED.status,
    attempts = EXCLUDED.attempts,
    processed_at = EXCLUDED.processed_at;