package br.com.terreiroreisebastiao.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_webhook_event")
public class EventoWebhookPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 20)
    private String provedor = "MERCADO_PAGO";

    @Column(name = "event_id", nullable = false, length = 120)
    private String idEvento;

    @Column(name = "event_type", nullable = false, length = 60)
    private String tipoEvento;

    @Column(name = "resource_id", nullable = false, length = 120)
    private String idRecurso;

    @Column(name = "signature", length = 255)
    private String assinatura;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "processed_at")
    private OffsetDateTime processadoEm;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String erroProcessamento;

    @Column(name = "attempts", nullable = false)
    private Short tentativas = 0;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime recebidoEm = OffsetDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected EventoWebhookPagamento() {}

    public EventoWebhookPagamento(String idEvento, String tipoEvento, String idRecurso, String assinatura, String payload) {
        this.idEvento = idEvento;
        this.tipoEvento = tipoEvento;
        this.idRecurso = idRecurso;
        this.assinatura = assinatura;
        this.payload = payload;
        this.tentativas = 0;
        this.recebidoEm = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getProvedor() { return provedor; }
    public String getIdEvento() { return idEvento; }
    public String getTipoEvento() { return tipoEvento; }
    public String getIdRecurso() { return idRecurso; }
    public String getAssinatura() { return assinatura; }
    public String getPayload() { return payload; }
    
    public OffsetDateTime getProcessadoEm() { return processadoEm; }
    public void setProcessadoEm(OffsetDateTime processadoEm) { this.processadoEm = processadoEm; }
    
    public String getErroProcessamento() { return erroProcessamento; }
    public void setErroProcessamento(String erroProcessamento) { this.erroProcessamento = erroProcessamento; }
    
    public Short getTentativas() { return tentativas; }
    public void incrementaTentativas() { this.tentativas++; }
}
