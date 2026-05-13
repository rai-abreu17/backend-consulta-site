package br.com.terreiroreisebastiao.availability.domain;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "availability_rule")
public class RegraDisponibilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Short weekday;

    @Column(name = "start_time", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "end_time", nullable = false)
    private LocalTime horaFim;

    @Column(name = "modalities", nullable = false, length = 40)
    private String modalidadesStr;

    @Column(name = "is_active", nullable = false)
    private Boolean ativo = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "updated_at")
    private OffsetDateTime atualizadoEm;

    public RegraDisponibilidade() {
    }

    public RegraDisponibilidade(Short weekday, LocalTime horaInicio, LocalTime horaFim, String modalidadesStr, Boolean ativo) {
        this.weekday = weekday;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.modalidadesStr = modalidadesStr;
        this.ativo = ativo;
    }

    public void atualizar(Short weekday, LocalTime horaInicio, LocalTime horaFim, String modalidadesStr, Boolean ativo) {
        this.weekday = weekday;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.modalidadesStr = modalidadesStr;
        this.ativo = ativo;
    }

    public UUID getId() { return id; }
    public Short getWeekday() { return weekday; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public Boolean getAtivo() { return ativo; }
    
    public Set<Modalidade> getModalidadesAsSet() {
        if (modalidadesStr == null || modalidadesStr.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(modalidadesStr.split(","))
                .map(String::trim)
                .map(Modalidade::valueOf)
                .collect(Collectors.toSet());
    }
}
