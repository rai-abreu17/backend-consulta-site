package br.com.terreiroreisebastiao.booking.controller;

import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import br.com.terreiroreisebastiao.booking.dto.BookingAdminDto;
import br.com.terreiroreisebastiao.booking.dto.BookingStatusUpdateRequest;
import br.com.terreiroreisebastiao.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/bookings", "/api/v1/admin/marcacoes"})
public class BookingAdminController {

    private final BookingService bookingService;

    public BookingAdminController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<Page<BookingAdminDto>> listarMarcacoes(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataInicio,
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataFim,
            @PageableDefault(size = 12, sort = "inicioEm") Pageable pageable) {

        OffsetDateTime filtroInicio = from != null ? from : dataInicio;
        OffsetDateTime filtroFim = to != null ? to : dataFim;

        return ResponseEntity.ok(
                bookingService.listarMarcacoesAdmin(status, filtroInicio, filtroFim, pageable)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> alterarEstadoMarcacao(
            @PathVariable UUID id,
            @Valid @RequestBody BookingStatusUpdateRequest request) {

        bookingService.alterarEstadoMarcacao(id, request.newStatus(), request.adminNotes());
        return ResponseEntity.noContent().build();
    }
}