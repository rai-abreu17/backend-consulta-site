package br.com.terreiroreisebastiao.booking.dto;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * DTO de resposta mínima para a criação pública de um booking em hold.
 */
public record RespostaCriacaoBooking(
        @JsonProperty("bookingId") UUID id,
        BookingStatus status,
        @JsonProperty("holdExpiresAt") OffsetDateTime holdExpiresAt,
        @JsonProperty("serviceId") UUID servicoId,
        @JsonProperty("startAt") OffsetDateTime inicioEm,
        @JsonProperty("endAt") OffsetDateTime fimEm,
        @JsonProperty("modality") Modalidade modalidade,
        @JsonProperty("initPointUrl") String checkoutUrl,
        @JsonProperty("viewToken") UUID viewToken
) {

    public static RespostaCriacaoBooking de(Booking booking, String checkoutUrl) {
        return new RespostaCriacaoBooking(
                booking.getId(),
                booking.getStatus(),
                normalizar(booking.getExpiracaoReservaEm()),
                booking.getServico().getId(),
                normalizar(booking.getInicioEm()),
                normalizar(booking.getFimEm()),
                booking.getModalidade(),
                checkoutUrl,
                booking.getViewToken()
        );
    }

    private static OffsetDateTime normalizar(OffsetDateTime valor) {
        return valor != null ? valor.withOffsetSameInstant(ZoneOffset.UTC) : null;
    }
}