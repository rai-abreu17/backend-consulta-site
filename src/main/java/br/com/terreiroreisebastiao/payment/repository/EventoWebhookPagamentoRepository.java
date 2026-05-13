package br.com.terreiroreisebastiao.payment.repository;

import br.com.terreiroreisebastiao.payment.domain.EventoWebhookPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventoWebhookPagamentoRepository extends JpaRepository<EventoWebhookPagamento, UUID> {
    Optional<EventoWebhookPagamento> findByProvedorAndIdEvento(String provedor, String idEvento);
}
