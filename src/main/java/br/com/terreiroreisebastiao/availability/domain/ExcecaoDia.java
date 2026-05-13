package br.com.terreiroreisebastiao.availability.domain;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "day_override")
public class ExcecaoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "date", nullable = false, unique = true)
    private LocalDate data;

    @Column(name = "is_closed", nullable = false)
    private Boolean fechado = false;

    @Column(name = "start_time")
    private LocalTime horaInicio;

    @Column(name = "end_time")
    private LocalTime horaFim;

    @Column(name = "modalities", length = 40)
    private String modalidadesStr;

    @Column(name = "reason", length = 280)
    private String motivo;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "updated_at")
    private OffsetDateTime atualizadoEm;

    public ExcecaoDia() {
    }

    public ExcecaoDia(LocalDate data, Boolean fechado, LocalTime horaInicio, LocalTime horaFim, String modalidadesStr, String motivo) {
        this.data = data;
        this.fechado = fechado;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.modalidadesStr = modalidadesStr;
        this.motivo = motivo;
    }
    
    public void atualizar(LocalDate data, Boolean fechado, LocalTime horaInicio, LocalTime horaFim, String modalidadesStr, String motivo) {
        this.data = data;
        this.fechado = fechado;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.modalidadesStr = modalidadesStr;
        this.motivo = motivo;
    }

    public UUID getId() { return id; }
    public LocalDate getData() { return data; }
    public Boolean getFechado() { return fechado; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    
    public String getMotivo() { return motivo; }

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
