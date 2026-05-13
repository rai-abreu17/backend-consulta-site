package br.com.terreiroreisebastiao.booking.repository;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MarcacaoRepository extends JpaRepository<Booking, UUID> {

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Booking m " +
           "WHERE m.status IN ('PENDING_PAYMENT', 'CONFIRMED') " +
           "AND m.inicioEm < :end AND m.fimEm > :start")
    boolean existsOverlappingActiveBooking(
            @Param("start") OffsetDateTime start, 
            @Param("end") OffsetDateTime end);

    @Query("SELECT m FROM Booking m " +
          "WHERE m.status IN ('PENDING_PAYMENT', 'CONFIRMED') " +
          "AND m.inicioEm < :fim " +
          "AND m.fimEm > :inicio " +
          "ORDER BY m.inicioEm ASC")
    List<Booking> listarMarcacoesAtivasNoIntervalo(
           @Param("inicio") OffsetDateTime inicio,
           @Param("fim") OffsetDateTime fim);
}
