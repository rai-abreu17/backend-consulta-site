-- Neutraliza o evento demo do outbox para evitar retries infinitos em ambientes sem SMTP.

UPDATE email_outbox_event
SET status = 'FAILED',
    attempts = CASE WHEN attempts < 3 THEN 3 ELSE attempts END,
    error_message = COALESCE(
        error_message,
        'Evento demo mantido apenas para exibicao administrativa; envio automatico desabilitado sem SMTP configurado.'
    ),
    processed_at = COALESCE(processed_at, now())
WHERE booking_id = '00000000-0000-0000-0000-000000000301'
  AND event_type = 'BOOKING_CONFIRMED'
  AND status = 'PENDING';