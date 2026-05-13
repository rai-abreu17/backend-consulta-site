package br.com.terreiroreisebastiao.booking.repository;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

       boolean existsByServicoId(UUID servicoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);
    
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.expiracaoReservaEm < :now")
    List<Booking> findExpiredHolds(@Param("status") BookingStatus status, @Param("now") OffsetDateTime now);

       @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.inicioEm >= :windowStart AND b.inicioEm < :windowEnd ORDER BY b.inicioEm ASC")
       List<Booking> findBookingsForReminderWindow(@Param("status") BookingStatus status,
                                                                                    @Param("windowStart") OffsetDateTime windowStart,
                                                                                    @Param("windowEnd") OffsetDateTime windowEnd);

    @Override
    @EntityGraph(attributePaths = {"servico", "consulente"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);
}
