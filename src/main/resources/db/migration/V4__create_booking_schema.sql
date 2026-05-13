-- V4__create_booking_schema.sql
-- Schema de agendamento/hold de slot.
-- Referencia apenas customer e service do domínio da aplicação.
-- Não referencia auth.users.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS booking (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  service_id            UUID         NOT NULL,
  customer_id           UUID         NOT NULL,
  modality              VARCHAR(20)  NOT NULL,
  start_at              TIMESTAMPTZ  NOT NULL,
  end_at                TIMESTAMPTZ  NOT NULL,
  status                VARCHAR(24)  NOT NULL,
  hold_expires_at       TIMESTAMPTZ,
  notes_admin           TEXT,
  cancel_reason         VARCHAR(280),
  price_snapshot_cents  BIGINT       NOT NULL,
  currency              CHAR(3)      NOT NULL DEFAULT 'BRL',
  created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT fk_booking_service
    FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE RESTRICT,
  CONSTRAINT fk_booking_customer
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE RESTRICT,
  CONSTRAINT chk_booking_modality
    CHECK (modality IN ('ONLINE', 'IN_PERSON')),
  CONSTRAINT chk_booking_status
    CHECK (
      status IN (
        'PENDING_PAYMENT',
        'CONFIRMED',
        'EXPIRED',
        'CANCELLED',
        'REFUNDED',
        'COMPLETED',
        'NO_SHOW'
      )
    ),
  CONSTRAINT chk_booking_time_range
    CHECK (start_at < end_at)
);

CREATE INDEX IF NOT EXISTS ix_booking_active_window
  ON booking USING gist (tstzrange(start_at, end_at, '[)'))
  WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED');

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'excl_booking_active_overlap'
      AND conrelid = 'booking'::regclass
  ) THEN
    ALTER TABLE booking
      ADD CONSTRAINT excl_booking_active_overlap
      EXCLUDE USING gist (
        tstzrange(start_at, end_at, '[)') WITH &&
      )
      WHERE (status IN ('PENDING_PAYMENT', 'CONFIRMED'));
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_booking_status
  ON booking(status);

CREATE INDEX IF NOT EXISTS ix_booking_start_at
  ON booking(start_at);

CREATE INDEX IF NOT EXISTS ix_booking_customer
  ON booking(customer_id);

CREATE INDEX IF NOT EXISTS ix_booking_service
  ON booking(service_id);

CREATE INDEX IF NOT EXISTS ix_booking_hold_exp
  ON booking(hold_expires_at)
  WHERE status = 'PENDING_PAYMENT';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_booking_updated'
      AND tgrelid = 'booking'::regclass
      AND NOT tgisinternal
  ) THEN
    EXECUTE 'CREATE TRIGGER trg_booking_updated
      BEFORE UPDATE ON booking
      FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
  END IF;
END $$;