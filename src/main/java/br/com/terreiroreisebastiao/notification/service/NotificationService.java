package br.com.terreiroreisebastiao.notification.service;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.notification.domain.EmailOutboxEvent;
import br.com.terreiroreisebastiao.notification.repository.EmailOutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public static final String EVENTO_CONFIRMED = "CONFIRMED";
    public static final String EVENTO_REMINDER_24H = "REMINDER_24H";

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailOutboxEventRepository outboxRepository;

    public NotificationService(EmailOutboxEventRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /**
     * Enfileira o envio de e-mail através do padrão Transactional Outbox.
     * Deve ser chamado dentro da mesma transação que altera o status do Booking.
     *
     * @param agendamento Agendamento persistido
     * @param tipoEvento PENDING_PAYMENT, CONFIRMED, EXPIRED, REFUNDED, REMINDER_24H
     */
    public boolean agendarEmail(Booking agendamento, String tipoEvento) {
        if (outboxRepository.existsByAgendamentoIdAndTipoEvento(agendamento.getId(), tipoEvento)) {
            log.info("Evento de e-mail já existente. bookingId={} tipoEvento={}", agendamento.getId(), tipoEvento);
            return false;
        }

        try {
            EmailOutboxEvent evento = new EmailOutboxEvent(agendamento, tipoEvento);
            outboxRepository.saveAndFlush(evento);
            log.info("Evento de e-mail enfileirado. bookingId={} tipoEvento={}", agendamento.getId(), tipoEvento);
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.info("Evento de e-mail deduplicado por constraint. bookingId={} tipoEvento={}", agendamento.getId(), tipoEvento);
            return false;
        }
    }

    public boolean agendarConfirmacao(Booking agendamento) {
        return agendarEmail(agendamento, EVENTO_CONFIRMED);
    }

    public boolean agendarLembrete24Horas(Booking agendamento) {
        return agendarEmail(agendamento, EVENTO_REMINDER_24H);
    }
}
