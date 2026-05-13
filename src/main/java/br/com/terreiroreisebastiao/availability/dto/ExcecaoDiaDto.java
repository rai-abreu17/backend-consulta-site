package br.com.terreiroreisebastiao.availability.dto;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.Set;

public record ExcecaoDiaDto(
        UUID id,
        LocalDate date,
        Boolean isClosed,
        LocalTime startTime,
        LocalTime endTime,
        Set<Modalidade> modalities,
        String reason
) {}
