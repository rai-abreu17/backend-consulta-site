package br.com.terreiroreisebastiao.booking.dto;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import br.com.terreiroreisebastiao.customer.domain.Customer;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingAdminDto(
    UUID id,
    @JsonProperty("serviceName") String nomeServico,
    @JsonProperty("customer") BookingAdminCustomerDto customer,
    @JsonProperty("modality") Modalidade modalidade,
    @JsonProperty("startAt") OffsetDateTime inicioEm,
    @JsonProperty("endAt") OffsetDateTime fimEm,
    BookingStatus status,
    @JsonProperty("priceCents") Long precoCents,
    @JsonProperty("currency") String moeda,
    @JsonProperty("adminNotes") String notasAdmin
) {
    public static BookingAdminDto fromEntity(Booking booking) {
        return new BookingAdminDto(
            booking.getId(),
            booking.getServico().getNome(),
            BookingAdminCustomerDto.fromEntity(booking.getConsulente()),
            booking.getModalidade(),
            booking.getInicioEm(),
            booking.getFimEm(),
            booking.getStatus(),
            booking.getPrecoCents(),
            booking.getMoeda(),
            booking.getNotasAdmin()
        );
    }

    public record BookingAdminCustomerDto(
        UUID id,
        @JsonProperty("fullName") String nomeCompleto,
        String email,
        String phone,
        @JsonProperty("isAnonymized") boolean anonimizado,
        @JsonProperty("shortId") String identificadorCurto
    ) {
        public static BookingAdminCustomerDto fromEntity(Customer customer) {
            String identificadorCurto = customer.getId().toString().substring(0, 8).toUpperCase();

            if (Boolean.TRUE.equals(customer.getAnonimizado())) {
                return new BookingAdminCustomerDto(
                    customer.getId(),
                    "Consulente anonimizado #" + identificadorCurto,
                    null,
                    null,
                    true,
                    identificadorCurto
                );
            }

            return new BookingAdminCustomerDto(
                customer.getId(),
                customer.getNomeCompleto(),
                customer.getEmail(),
                customer.getTelefone(),
                false,
                identificadorCurto
            );
        }
    }
}
