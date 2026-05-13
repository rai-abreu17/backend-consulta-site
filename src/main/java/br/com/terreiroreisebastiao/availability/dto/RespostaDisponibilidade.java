package br.com.terreiroreisebastiao.availability.dto;

import java.util.List;
import java.util.UUID;

public record RespostaDisponibilidade(
        UUID serviceId,
        String timezone,
        List<SlotDto> slots
) {
}
