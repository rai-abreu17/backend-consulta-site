package br.com.terreiroreisebastiao.payment.repository;

import br.com.terreiroreisebastiao.payment.domain.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
    Optional<Pagamento> findByAgendamentoId(UUID bookingId);

    Optional<Pagamento> findByIdPagamentoProvedor(String idPagamentoProvedor);
}
