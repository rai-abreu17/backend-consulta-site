package br.com.terreiroreisebastiao.booking.domain;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import br.com.terreiroreisebastiao.catalog.domain.Servico;
import br.com.terreiroreisebastiao.customer.domain.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Entidade que representa um Agendamento (ou Tiragem/Jogo) do Terreiro de Rei Sebastião.
 * 
 * Substitui o antigo Marcacao.java.
 */
@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Servico servico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer consulente;

    @Enumerated(EnumType.STRING)
    @Column(name = "modality", nullable = false, length = 20)
    private Modalidade modalidade;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime inicioEm;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime fimEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private BookingStatus status;

    @Column(name = "hold_expires_at")
    private OffsetDateTime expiracaoReservaEm;

    @Column(name = "notes_admin", columnDefinition = "TEXT")
    private String notasAdmin;

    @Column(name = "cancel_reason", length = 280)
    private String motivoCancelamento;

    @Column(name = "price_snapshot_cents", nullable = false)
    private Long precoCents;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String moeda = "BRL";

    @Column(name = "view_token", nullable = false, updatable = false)
    private UUID viewToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Booking() {
    }

    public Booking(Servico servico, Customer consulente, Modalidade modalidade, 
                   OffsetDateTime inicioEm, OffsetDateTime fimEm, BookingStatus status, 
                   OffsetDateTime expiracaoReservaEm, Long precoCents) {
        this.servico = servico;
        this.consulente = consulente;
        this.modalidade = modalidade;
        this.inicioEm = inicioEm;
        this.fimEm = fimEm;
        this.status = status;
        this.expiracaoReservaEm = expiracaoReservaEm;
        this.precoCents = precoCents;
        this.moeda = "BRL";
        this.viewToken = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public Servico getServico() {
        return servico;
    }

    public void setServico(Servico servico) {
        this.servico = servico;
    }

    public Customer getConsulente() {
        return consulente;
    }

    public void setConsulente(Customer consulente) {
        this.consulente = consulente;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public void setModalidade(Modalidade modalidade) {
        this.modalidade = modalidade;
    }

    public OffsetDateTime getInicioEm() {
        return inicioEm;
    }

    public void setInicioEm(OffsetDateTime inicioEm) {
        this.inicioEm = inicioEm;
    }

    public OffsetDateTime getFimEm() {
        return fimEm;
    }

    public void setFimEm(OffsetDateTime fimEm) {
        this.fimEm = fimEm;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public OffsetDateTime getExpiracaoReservaEm() {
        return expiracaoReservaEm;
    }

    public void setExpiracaoReservaEm(OffsetDateTime expiracaoReservaEm) {
        this.expiracaoReservaEm = expiracaoReservaEm;
    }

    public String getNotasAdmin() {
        return notasAdmin;
    }

    public void setNotasAdmin(String notasAdmin) {
        this.notasAdmin = notasAdmin;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public Long getPrecoCents() {
        return precoCents;
    }

    public void setPrecoCents(Long precoCents) {
        this.precoCents = precoCents;
    }

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }

    public UUID getViewToken() {
        return viewToken;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    @PrePersist
    void prepararPersistencia() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        if (criadoEm == null) {
            criadoEm = agora;
        }
        atualizadoEm = agora;
    }

    @PreUpdate
    void prepararAtualizacao() {
        atualizadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
