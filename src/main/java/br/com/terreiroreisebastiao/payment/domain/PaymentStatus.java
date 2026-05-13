package br.com.terreiroreisebastiao.payment.domain;

public enum PaymentStatus {
    CREATED,
    PENDING,
    APPROVED,
    REJECTED,
    REFUNDED,
    CANCELLED,
    EXPIRED
}
