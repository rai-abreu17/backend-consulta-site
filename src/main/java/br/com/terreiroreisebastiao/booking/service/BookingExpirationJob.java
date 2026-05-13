package br.com.terreiroreisebastiao.booking.service;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import br.com.terreiroreisebastiao.booking.repository.BookingRepository;
import br.com.terreiroreisebastiao.payment.domain.Pagamento;
import br.com.terreiroreisebastiao.payment.domain.PaymentStatus;
import br.com.terreiroreisebastiao.payment.repository.PagamentoRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Job responsável por liberar slots de agendamento cujo tempo limite de pagamento expirou.
 */
@Service
public class BookingExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpirationJob.class);

    private final BookingRepository bookingRepository;
    private final PagamentoRepository pagamentoRepository;
    private final RedissonClient redissonClient;

    public BookingExpirationJob(BookingRepository bookingRepository,
                                PagamentoRepository pagamentoRepository,
                                RedissonClient redissonClient) {
        this.bookingRepository = bookingRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.redissonClient = redissonClient;
    }

    /**
     * Roda a cada 60 segundos buscando holds expirados.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireHolds() {
        OffsetDateTime agora = OffsetDateTime.now();
        
        // Utiliza o método com Lock Pessimista (FOR UPDATE SKIP LOCKED)
        List<Booking> expirados = bookingRepository.findExpiredHolds(BookingStatus.PENDING_PAYMENT, agora);

        if (expirados.isEmpty()) {
            return;
        }

        int expiradosCount = 0;

        for (Booking booking : expirados) {
            String lockKey = "booking:" + booking.getId();
            RLock lock = redissonClient.getLock(lockKey);
            
            boolean locked = false;
            try {
                locked = lock.tryLock(1, 30, TimeUnit.SECONDS);
                
                if (locked) {
                    booking.setStatus(BookingStatus.EXPIRED);
                    bookingRepository.save(booking);

                    Pagamento pagamento = pagamentoRepository.findByAgendamentoId(booking.getId()).orElse(null);
                    if (pagamento != null) {
                        pagamento.setStatus(PaymentStatus.EXPIRED);
                        pagamentoRepository.save(pagamento);
                    }
                    
                    expiradosCount++;
                    log.info("Agendamento {} expirado por falta de pagamento.", booking.getId());
                } else {
                    log.debug("Agendamento {} sendo processado por Webhook. Ignorando no Job.", booking.getId());
                }
            } catch (Exception e) {
                log.error("Erro ao expirar agendamento {}", booking.getId(), e);
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        if (expiradosCount > 0) {
            log.info("Expirados {} agendamentos neste ciclo.", expiradosCount);
        }
    }
}
