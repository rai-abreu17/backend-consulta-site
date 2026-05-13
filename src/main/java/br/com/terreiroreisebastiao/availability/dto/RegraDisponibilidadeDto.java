package br.com.terreiroreisebastiao.availability.dto;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;

import java.time.LocalTime;
import java.util.UUID;
import java.util.Set;

public record RegraDisponibilidadeDto(
        UUID id,
        Short weekday,
        LocalTime startTime,
        LocalTime endTime,
        Set<Modalidade> modalities,
        Boolean isActive
) {}
