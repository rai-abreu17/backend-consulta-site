-- V8__create_email_outbox_schema.sql
-- Outbox transacional para notificações por e-mail.

CREATE TABLE IF NOT EXISTS email_outbox_event (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id      UUID         NOT NULL,
  event_type      VARCHAR(50)  NOT NULL,
  status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  error_message   TEXT,
  attempts        SMALLINT     NOT NULL DEFAULT 0,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  processed_at    TIMESTAMPTZ,
  CONSTRAINT fk_email_outbox_booking
    FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE,
  CONSTRAINT uq_email_outbox_booking_event
    UNIQUE (booking_id, event_type),
  CONSTRAINT chk_email_outbox_status
    CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
  CONSTRAINT chk_email_outbox_attempts
    CHECK (attempts >= 0)
);

CREATE INDEX IF NOT EXISTS ix_email_outbox_pending
  ON email_outbox_event(status, created_at)
  WHERE status = 'PENDING';