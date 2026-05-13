package br.com.terreiroreisebastiao.booking.service;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import br.com.terreiroreisebastiao.booking.repository.BookingRepository;
import br.com.terreiroreisebastiao.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Job agendado que varre o banco de dados buscando consultas
 * CONFIRMADAS programadas para as próximas 24 horas, e dispara o e-mail de lembrete.
 */
@Service
public class BookingReminderJob {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderJob.class);

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public BookingReminderJob(BookingRepository bookingRepository, NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    /**
     * Executa a cada hora para disparar os e-mails 24 horas antes do início da sessão.
     */
    @Scheduled(cron = "0 0 * * * *") // Executa no minuto 0 de cada hora
    @Transactional
    public void sendReminders() {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime alvoInicio = agora.plusHours(24);
        OffsetDateTime alvoFim = agora.plusHours(25);
        
        log.info("Buscando consultas para enviar lembrete de 24h...");

        List<Booking> bookingsParaLembrar = bookingRepository.findBookingsForReminderWindow(
                BookingStatus.CONFIRMED,
                alvoInicio,
                alvoFim
        );

        for (Booking booking : bookingsParaLembrar) {
            try {
                boolean enfileirado = notificationService.agendarLembrete24Horas(booking);
                if (enfileirado) {
                    log.info("Lembrete 24h enfileirado. bookingId={} inicioEm={}", booking.getId(), booking.getInicioEm());
                } else {
                    log.info("Lembrete 24h já existia. bookingId={} inicioEm={}", booking.getId(), booking.getInicioEm());
                }
            } catch (Exception e) {
                log.error("Erro ao enfileirar lembrete 24h. bookingId={} motivo={}", booking.getId(), e.getMessage());
            }
        }
    }
}
