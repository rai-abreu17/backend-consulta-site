package br.com.terreiroreisebastiao.availability.controller;

import br.com.terreiroreisebastiao.availability.dto.ExcecaoDiaDto;
import br.com.terreiroreisebastiao.availability.dto.RegraDisponibilidadeDto;
import br.com.terreiroreisebastiao.availability.dto.SalvarExcecaoDiaRequest;
import br.com.terreiroreisebastiao.availability.dto.SalvarRegraDisponibilidadeRequest;
import br.com.terreiroreisebastiao.availability.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/rules")
    public ResponseEntity<List<RegraDisponibilidadeDto>> listarRegrasSemanais() {
        return ResponseEntity.ok(availabilityService.buscarRegrasSemanais());
    }

    @PostMapping("/rules")
    public ResponseEntity<RegraDisponibilidadeDto> criarRegraSemanal(
            @RequestBody @Valid SalvarRegraDisponibilidadeRequest requisicao) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(availabilityService.criarRegraSemanal(requisicao));
    }

    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<RegraDisponibilidadeDto> atualizarRegraSemanal(
            @PathVariable("ruleId") UUID ruleId,
            @RequestBody @Valid SalvarRegraDisponibilidadeRequest requisicao) {
        return ResponseEntity.ok(availabilityService.atualizarRegraSemanal(ruleId, requisicao));
    }

    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> removerRegraSemanal(@PathVariable("ruleId") UUID ruleId) {
        availabilityService.removerRegraSemanal(ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/overrides")
    public ResponseEntity<List<ExcecaoDiaDto>> listarExcecoes(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(availabilityService.buscarExcecoes(from, to));
    }

    @PostMapping("/overrides")
    public ResponseEntity<ExcecaoDiaDto> criarExcecaoDia(
            @RequestBody @Valid SalvarExcecaoDiaRequest requisicao) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(availabilityService.criarExcecaoDia(requisicao));
    }

    @PutMapping("/overrides/{overrideId}")
    public ResponseEntity<ExcecaoDiaDto> atualizarExcecaoDia(
            @PathVariable("overrideId") UUID overrideId,
            @RequestBody @Valid SalvarExcecaoDiaRequest requisicao) {
        return ResponseEntity.ok(availabilityService.atualizarExcecaoDia(overrideId, requisicao));
    }

    @DeleteMapping("/overrides/{overrideId}")
    public ResponseEntity<Void> removerExcecaoDia(
            @PathVariable("overrideId") UUID overrideId) {
        availabilityService.removerExcecaoDia(overrideId);
        return ResponseEntity.noContent().build();
    }
}
