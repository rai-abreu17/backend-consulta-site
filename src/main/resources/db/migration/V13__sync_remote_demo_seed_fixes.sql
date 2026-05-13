-- Corrige dados demo em bancos onde V10/V11 ja foram aplicadas antes dos ajustes locais.

UPDATE booking
SET modality = 'ONLINE',
    updated_at = now()
WHERE id = '00000000-0000-0000-0000-000000000301'
  AND modality <> 'ONLINE';

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