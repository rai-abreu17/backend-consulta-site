package br.com.terreiroreisebastiao.availability.controller;

import br.com.terreiroreisebastiao.availability.dto.RespostaDisponibilidade;
import br.com.terreiroreisebastiao.availability.dto.SlotDto;
import br.com.terreiroreisebastiao.availability.service.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/availability")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping("/slots")
    public ResponseEntity<RespostaDisponibilidade> buscarSlots(
            @RequestParam UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "ANY") String modality) {
        
        List<SlotDto> slots = slotService.gerarSlots(serviceId, from, to, modality);
        
        RespostaDisponibilidade resposta = new RespostaDisponibilidade(
                serviceId,
            "America/Fortaleza",
                slots
        );
        
        return ResponseEntity.ok(resposta);
    }
}
