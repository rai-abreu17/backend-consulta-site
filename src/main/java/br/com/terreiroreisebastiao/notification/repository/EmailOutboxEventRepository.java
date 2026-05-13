package br.com.terreiroreisebastiao.notification.repository;

import br.com.terreiroreisebastiao.notification.domain.EmailOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailOutboxEventRepository extends JpaRepository<EmailOutboxEvent, UUID> {

    @Query(value = "SELECT * FROM email_outbox_event WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 50 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<EmailOutboxEvent> findPendingEventsForUpdate();

    boolean existsByAgendamentoIdAndTipoEvento(UUID bookingId, String tipoEvento);
}
