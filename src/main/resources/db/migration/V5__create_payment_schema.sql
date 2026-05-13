-- V5__create_payment_schema.sql
-- Schema de pagamento e registro idempotente de webhooks.
-- Não referencia auth.users.

CREATE TABLE IF NOT EXISTS payment (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id           UUID         NOT NULL,
  provider             VARCHAR(20)  NOT NULL DEFAULT 'MERCADO_PAGO',
  provider_pref_id     VARCHAR(120),
  provider_payment_id  VARCHAR(120),
  status               VARCHAR(24)  NOT NULL,
  amount_cents         BIGINT       NOT NULL,
  currency             CHAR(3)      NOT NULL DEFAULT 'BRL',
  init_point_url       TEXT,
  raw_response         JSONB,
  approved_at          TIMESTAMPTZ,
  refunded_at          TIMESTAMPTZ,
  created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_payment_booking UNIQUE (booking_id),
  CONSTRAINT fk_payment_booking
    FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE,
  CONSTRAINT chk_payment_status
    CHECK (
      status IN (
        'CREATED',
        'PENDING',
        'APPROVED',
        'REJECTED',
        'REFUNDED',
        'CANCELLED',
        'EXPIRED'
      )
    ),
  CONSTRAINT chk_payment_amount_cents
    CHECK (amount_cents >= 0)
);

CREATE INDEX IF NOT EXISTS ix_payment_status
  ON payment(status);

CREATE INDEX IF NOT EXISTS ix_payment_provider_pref
  ON payment(provider_pref_id);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_payment_updated'
      AND tgrelid = 'payment'::regclass
      AND NOT tgisinternal
  ) THEN
    EXECUTE 'CREATE TRIGGER trg_payment_updated
      BEFORE UPDATE ON payment
      FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS payment_webhook_event (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  provider          VARCHAR(20)  NOT NULL DEFAULT 'MERCADO_PAGO',
  event_id          VARCHAR(120) NOT NULL,
  event_type        VARCHAR(60)  NOT NULL,
  resource_id       VARCHAR(120) NOT NULL,
  signature         VARCHAR(255),
  payload           JSONB        NOT NULL,
  processed_at      TIMESTAMPTZ,
  processing_error  TEXT,
  attempts          SMALLINT     NOT NULL DEFAULT 0,
  received_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_webhook_event UNIQUE (provider, event_id)
);

CREATE INDEX IF NOT EXISTS ix_webhook_unprocessed
  ON payment_webhook_event(received_at)
  WHERE processed_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_webhook_event_updated'
      AND tgrelid = 'payment_webhook_event'::regclass
      AND NOT tgisinternal
  ) THEN
    EXECUTE 'CREATE TRIGGER trg_webhook_event_updated
      BEFORE UPDATE ON payment_webhook_event
      FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
  END IF;
END $$;