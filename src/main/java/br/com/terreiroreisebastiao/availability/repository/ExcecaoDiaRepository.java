package br.com.terreiroreisebastiao.availability.repository;

import br.com.terreiroreisebastiao.availability.domain.ExcecaoDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExcecaoDiaRepository extends JpaRepository<ExcecaoDia, UUID> {
    Optional<ExcecaoDia> findByData(LocalDate data);
    List<ExcecaoDia> findByDataBetween(LocalDate from, LocalDate to);
    boolean existsByDataAndIdNot(LocalDate data, UUID id);
}
