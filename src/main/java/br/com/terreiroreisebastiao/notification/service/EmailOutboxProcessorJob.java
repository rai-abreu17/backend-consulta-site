package br.com.terreiroreisebastiao.notification.service;

import br.com.terreiroreisebastiao.notification.domain.EmailOutboxEvent;
import br.com.terreiroreisebastiao.notification.repository.EmailOutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Job responsável por varrer a fila Outbox de e-mails de forma transacional 
 * e disparar as mensagens usando SMTP / Template Engine.
 */
@Service
public class EmailOutboxProcessorJob {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxProcessorJob.class);

    private final EmailOutboxEventRepository outboxRepository;
    private final EmailService emailService;

    public EmailOutboxProcessorJob(EmailOutboxEventRepository outboxRepository,
                                   EmailService emailService) {
        this.outboxRepository = outboxRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<EmailOutboxEvent> pendingEvents = outboxRepository.findPendingEventsForUpdate();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processando {} e-mails pendentes no Outbox...", pendingEvents.size());

        for (EmailOutboxEvent evento : pendingEvents) {
            try {
                emailService.enviar(evento);
                
                evento.setStatus("SENT");
                evento.setProcessadoEm(OffsetDateTime.now());
                evento.setMensagemErro(null);
                log.info("event=EMAIL_SENT eventoId={} bookingId={} template={} tipoEvento={}",
                        evento.getId(),
                        evento.getAgendamento().getId(),
                        emailService.resolverTemplate(evento.getTipoEvento()),
                        evento.getTipoEvento());
                         
            } catch (Exception e) {
                log.error("event=EMAIL_SEND_FAILED eventoId={} bookingId={} tipoEvento={} motivo={}",
                        evento.getId(),
                        evento.getAgendamento().getId(),
                        evento.getTipoEvento(),
                        e.getMessage());
                evento.incrementarTentativas();
                evento.setMensagemErro(e.getMessage());
                
                if (evento.getTentativas() >= 3) {
                    evento.setStatus("FAILED");
                }
            }
            outboxRepository.save(evento);
        }
    }
}
