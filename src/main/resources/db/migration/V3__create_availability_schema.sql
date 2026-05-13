-- V3__create_availability_schema.sql
-- Schema de disponibilidade e overrides pontuais.
-- Não referencia auth.users.

CREATE TABLE IF NOT EXISTS availability_rule (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  weekday     SMALLINT    NOT NULL,
  start_time  TIME        NOT NULL,
  end_time    TIME        NOT NULL,
  modalities  VARCHAR(40) NOT NULL,
  is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_availability_rule_weekday
    CHECK (weekday BETWEEN 0 AND 6),
  CONSTRAINT chk_availability_rule_time_range
    CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS ix_availability_weekday
  ON availability_rule(weekday)
  WHERE is_active = TRUE;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_availability_rule_updated'
      AND tgrelid = 'availability_rule'::regclass
      AND NOT tgisinternal
  ) THEN
    EXECUTE 'CREATE TRIGGER trg_availability_rule_updated
      BEFORE UPDATE ON availability_rule
      FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS day_override (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  date        DATE        NOT NULL,
  is_closed   BOOLEAN     NOT NULL DEFAULT FALSE,
  start_time  TIME,
  end_time    TIME,
  modalities  VARCHAR(40),
  reason      VARCHAR(280),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_day_override_date UNIQUE (date),
  CONSTRAINT chk_day_override_consistency
    CHECK (
      is_closed = TRUE
      OR (
        start_time IS NOT NULL
        AND end_time IS NOT NULL
        AND start_time < end_time
      )
    )
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_day_override_updated'
      AND tgrelid = 'day_override'::regclass
      AND NOT tgisinternal
  ) THEN
    EXECUTE 'CREATE TRIGGER trg_day_override_updated
      BEFORE UPDATE ON day_override
      FOR EACH ROW EXECUTE FUNCTION set_updated_at()';
  END IF;
END $$;