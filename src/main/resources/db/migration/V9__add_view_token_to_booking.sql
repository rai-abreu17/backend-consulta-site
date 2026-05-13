-- V9__add_view_token_to_booking.sql
-- Adiciona coluna view_token para acesso seguro ao status do booking sem login.
-- Existindo linhas, gen_random_uuid() garante unicidade retroativa.

ALTER TABLE booking
  ADD COLUMN IF NOT EXISTS view_token UUID NOT NULL DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX IF NOT EXISTS ix_booking_view_token
  ON booking(view_token);
