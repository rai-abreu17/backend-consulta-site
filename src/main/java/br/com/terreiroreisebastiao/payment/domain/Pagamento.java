package br.com.terreiroreisebastiao.payment.domain;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking agendamento;

    @Column(name = "provider", nullable = false, length = 20)
    private String provedor = "MERCADO_PAGO";

    @Column(name = "provider_pref_id", length = 120)
    private String idPreferenciaProvedor;

    @Column(name = "provider_payment_id", length = 120)
    private String idPagamentoProvedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PaymentStatus status;

    @Column(name = "amount_cents", nullable = false)
    private Long valorCents;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String moeda = "BRL";

    @Column(name = "init_point_url", columnDefinition = "TEXT")
    private String urlCheckout;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String respostaBruta;

    @Column(name = "approved_at")
    private OffsetDateTime aprovadoEm;

    @Column(name = "refunded_at")
    private OffsetDateTime estornadoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    public Pagamento() {}

    @PrePersist
    void prepararPersistencia() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        if (criadoEm == null) criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void prepararAtualizacao() {
        atualizadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public Booking getAgendamento() { return agendamento; }
    public void setAgendamento(Booking agendamento) { this.agendamento = agendamento; }
    public String getProvedor() { return provedor; }
    public void setProvedor(String provedor) { this.provedor = provedor; }
    public String getIdPreferenciaProvedor() { return idPreferenciaProvedor; }
    public void setIdPreferenciaProvedor(String idPreferenciaProvedor) { this.idPreferenciaProvedor = idPreferenciaProvedor; }
    public String getIdPagamentoProvedor() { return idPagamentoProvedor; }
    public void setIdPagamentoProvedor(String idPagamentoProvedor) { this.idPagamentoProvedor = idPagamentoProvedor; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public Long getValorCents() { return valorCents; }
    public void setValorCents(Long valorCents) { this.valorCents = valorCents; }
    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
    public String getUrlCheckout() { return urlCheckout; }
    public void setUrlCheckout(String urlCheckout) { this.urlCheckout = urlCheckout; }
    public String getRespostaBruta() { return respostaBruta; }
    public void setRespostaBruta(String respostaBruta) { this.respostaBruta = respostaBruta; }
    public OffsetDateTime getAprovadoEm() { return aprovadoEm; }
    public void setAprovadoEm(OffsetDateTime aprovadoEm) { this.aprovadoEm = aprovadoEm; }
    public OffsetDateTime getEstornadoEm() { return estornadoEm; }
    public void setEstornadoEm(OffsetDateTime estornadoEm) { this.estornadoEm = estornadoEm; }
}
