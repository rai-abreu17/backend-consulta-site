package br.com.terreiroreisebastiao.availability.dto;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.Set;

public record SalvarRegraDisponibilidadeRequest(

        @NotNull(message = "O dia da semana é obrigatório.")
        @Min(value = 0, message = "O dia da semana deve estar entre 0 e 6.")
        @Max(value = 6, message = "O dia da semana deve estar entre 0 e 6.")
        Short weekday,

        @NotNull(message = "O horário inicial é obrigatório.")
        LocalTime startTime,

        @NotNull(message = "O horário final é obrigatório.")
        LocalTime endTime,

        @NotEmpty(message = "Selecione pelo menos uma modalidade.")
        Set<Modalidade> modalities,

        Boolean isActive
) {
}