package br.com.terreiroreisebastiao.availability.repository;

import br.com.terreiroreisebastiao.availability.domain.RegraDisponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RegraDisponibilidadeRepository extends JpaRepository<RegraDisponibilidade, UUID> {
    List<RegraDisponibilidade> findByWeekdayAndAtivoTrue(Short weekday);
}
