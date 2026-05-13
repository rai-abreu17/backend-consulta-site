package br.com.terreiroreisebastiao.booking.service;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import br.com.terreiroreisebastiao.booking.repository.BookingRepository;
import br.com.terreiroreisebastiao.payment.domain.Pagamento;
import br.com.terreiroreisebastiao.payment.domain.PaymentStatus;
import br.com.terreiroreisebastiao.payment.repository.PagamentoRepository;
import br.com.terreiroreisebastiao.shared.lock.LockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class BookingExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpirationJob.class);

    private final BookingRepository bookingRepository;
    private final PagamentoRepository pagamentoRepository;
    private final LockService lockService;

    public BookingExpirationJob(BookingRepository bookingRepository,
                                PagamentoRepository pagamentoRepository,
                                LockService lockService) {
        this.bookingRepository = bookingRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.lockService = lockService;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireHolds() {
        OffsetDateTime agora = OffsetDateTime.now();
        List<Booking> expirados = bookingRepository.findExpiredHolds(BookingStatus.PENDING_PAYMENT, agora);

        if (expirados.isEmpty()) {
            return;
        }

        int expiradosCount = 0;

        for (Booking booking : expirados) {
            String lockKey = "booking:" + booking.getId();
            try {
                boolean locked = lockService.executeWithLock(lockKey, 1, () -> {
                    booking.setStatus(BookingStatus.EXPIRED);
                    bookingRepository.save(booking);

                    Pagamento pagamento = pagamentoRepository.findByAgendamentoId(booking.getId()).orElse(null);
                    if (pagamento != null) {
                        pagamento.setStatus(PaymentStatus.EXPIRED);
                        pagamentoRepository.save(pagamento);
                    }

                    log.info("Agendamento {} expirado por falta de pagamento.", booking.getId());
                });

                if (locked) {
                    expiradosCount++;
                } else {
                    log.debug("Agendamento {} sendo processado por Webhook. Ignorando no Job.", booking.getId());
                }
            } catch (Exception e) {
                log.error("Erro ao expirar agendamento {}", booking.getId(), e);
            }
        }

        if (expiradosCount > 0) {
            log.info("Expirados {} agendamentos neste ciclo.", expiradosCount);
        }
    }
}
