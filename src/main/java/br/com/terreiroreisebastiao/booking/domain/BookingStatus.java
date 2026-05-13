package br.com.terreiroreisebastiao.booking.domain;

/**
 * Enum que define o status da máquina de estados de um Agendamento (Booking).
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    EXPIRED,
    CANCELLED,
    REFUNDED,
    COMPLETED,
    NO_SHOW
}
