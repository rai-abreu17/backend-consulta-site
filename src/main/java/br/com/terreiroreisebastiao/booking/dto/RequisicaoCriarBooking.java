package br.com.terreiroreisebastiao.booking.dto;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de entrada do endpoint público que inicia uma reserva em {@code PENDING_PAYMENT}.
 */
public record RequisicaoCriarBooking(

        @NotNull(message = "O serviço é obrigatório.")
        @JsonProperty("serviceId")
        UUID servicoId,

        @NotNull(message = "O horário inicial é obrigatório.")
        @JsonProperty("startAt")
        OffsetDateTime inicioEm,

        @NotNull(message = "A modalidade é obrigatória.")
        @JsonProperty("modality")
        Modalidade modalidade,

        @NotNull(message = "Os dados do consulente são obrigatórios.")
        @Valid
        @JsonProperty("customer")
        DadosConsulente consulente
) {

    public record DadosConsulente(

            @NotBlank(message = "O nome completo é obrigatório.")
            @Size(min = 3, max = 120, message = "O nome completo deve ter entre 3 e 120 caracteres.")
            @JsonProperty("fullName")
            String nomeCompleto,

            @NotBlank(message = "O e-mail é obrigatório.")
            @Email(message = "O e-mail informado é inválido.")
            @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
            String email,

            @NotBlank(message = "O telefone é obrigatório.")
            @Pattern(regexp = "^\\+[1-9]\\d{10,14}$", message = "O telefone deve estar no formato E.164.")
            String phone
    ) {
    }
}