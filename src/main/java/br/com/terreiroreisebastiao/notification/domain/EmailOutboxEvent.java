package br.com.terreiroreisebastiao.notification.domain;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_outbox_event")
public class EmailOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking agendamento;

    @Column(name = "event_type", nullable = false, length = 50)
    private String tipoEvento; // PENDING_PAYMENT, CONFIRMED, EXPIRED, REFUNDED, REMINDER_24H

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, SENT, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String mensagemErro;

    @Column(name = "attempts", nullable = false)
    private Short tentativas = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "processed_at")
    private OffsetDateTime processadoEm;

    protected EmailOutboxEvent() {}

    public EmailOutboxEvent(Booking agendamento, String tipoEvento) {
        this.agendamento = agendamento;
        this.tipoEvento = tipoEvento;
        this.status = "PENDING";
        this.tentativas = 0;
        this.criadoEm = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Booking getAgendamento() { return agendamento; }
    public String getTipoEvento() { return tipoEvento; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMensagemErro() { return mensagemErro; }
    public void setMensagemErro(String mensagemErro) { this.mensagemErro = mensagemErro; }
    public Short getTentativas() { return tentativas; }
    public void incrementarTentativas() { this.tentativas++; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getProcessadoEm() { return processadoEm; }
    public void setProcessadoEm(OffsetDateTime processadoEm) { this.processadoEm = processadoEm; }
}
