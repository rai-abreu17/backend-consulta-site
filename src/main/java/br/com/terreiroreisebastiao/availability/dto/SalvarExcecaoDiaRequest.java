package br.com.terreiroreisebastiao.availability.dto;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record SalvarExcecaoDiaRequest(

        @NotNull(message = "A data da exceção é obrigatória.")
        LocalDate date,

        @NotNull(message = "Informe se o dia estará fechado.")
        Boolean isClosed,

        LocalTime startTime,

        LocalTime endTime,

        Set<Modalidade> modalities,

        @Size(max = 280, message = "O motivo deve ter no máximo 280 caracteres.")
        String reason
) {
}