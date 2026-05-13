package br.com.terreiroreisebastiao.booking.dto;

import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record BookingStatusUpdateRequest(
    @JsonProperty("new_status")
    @JsonAlias({"status", "newStatus"})
    @NotNull(message = "O novo status é obrigatório.")
    BookingStatus newStatus,

    @JsonProperty("admin_notes")
    @JsonAlias({"notasAdmin", "adminNotes"})
    String adminNotes
) {}
