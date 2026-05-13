package br.com.terreiroreisebastiao.availability.dto;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;

import java.time.OffsetDateTime;
import java.util.Set;

public record SlotDto(
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        Set<Modalidade> modalitiesAvailable
) {
}
