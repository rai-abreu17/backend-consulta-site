package br.com.terreiroreisebastiao.booking.controller;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import br.com.terreiroreisebastiao.booking.dto.RequisicaoCriarBooking;
import br.com.terreiroreisebastiao.booking.dto.RespostaCriacaoBooking;
import br.com.terreiroreisebastiao.booking.service.BookingService;
import br.com.terreiroreisebastiao.booking.service.BookingService.ResultadoReservaPendente;
import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Endpoints públicos de booking: criação do hold e polling de status para o frontend.
 */
@RestController
@RequestMapping({"/api/v1/public/bookings", "/api/v1/bookings"})
public class BookingPublicoController {

    private record ServicoResumo(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String nome,
            @JsonProperty("durationMin") int duracaoMinutos
    ) {}

    private record ValorResumo(
            @JsonProperty("cents") long centavos,
            @JsonProperty("formatted") String formatado,
            @JsonProperty("currency") String moeda
    ) {}

    private record StatusBookingResposta(
            @JsonProperty("bookingId") UUID id,
            BookingStatus status,
            @JsonProperty("holdExpiresAt") OffsetDateTime holdExpiresAt,
            @JsonProperty("startAt") OffsetDateTime startAt,
            @JsonProperty("endAt") OffsetDateTime endAt,
            @JsonProperty("modality") Modalidade modality,
            @JsonProperty("service") ServicoResumo service,
            @JsonProperty("amount") ValorResumo amount,
            @JsonProperty("viewToken") UUID viewToken
    ) {
        static StatusBookingResposta de(Booking booking) {
            ServicoResumo servico = new ServicoResumo(
                    booking.getServico().getId(),
                    booking.getServico().getNome(),
                    booking.getServico().getDuracaoMinutos().intValue()
            );
            long centavos = booking.getPrecoCents() != null ? booking.getPrecoCents() : 0L;
            String formatado = String.format("R$ %,.2f", centavos / 100.0)
                    .replace('.', '#').replace(',', '.').replace('#', ',');
            ValorResumo valor = new ValorResumo(centavos, formatado, booking.getMoeda());

            return new StatusBookingResposta(
                    booking.getId(),
                    booking.getStatus(),
                    normalizar(booking.getExpiracaoReservaEm()),
                    normalizar(booking.getInicioEm()),
                    normalizar(booking.getFimEm()),
                    booking.getModalidade(),
                    servico,
                    valor,
                    booking.getViewToken()
            );
        }

        private static OffsetDateTime normalizar(OffsetDateTime valor) {
            return valor != null ? valor.withOffsetSameInstant(ZoneOffset.UTC) : null;
        }
    }

    private final BookingService bookingService;

    public BookingPublicoController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<RespostaCriacaoBooking> criarBooking(
            @Valid @RequestBody RequisicaoCriarBooking requisicao) {

        ResultadoReservaPendente resultado = bookingService.criarReservaPendente(
                requisicao.servicoId(),
                requisicao.inicioEm(),
                requisicao.modalidade(),
                requisicao.consulente().nomeCompleto(),
                requisicao.consulente().email(),
                requisicao.consulente().phone()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RespostaCriacaoBooking.de(resultado.booking(), resultado.checkoutUrl()));
    }

    /**
     * Polling de status para a tela BookingStatusPage do frontend.
     * O cliente consulta este endpoint a cada 5 s até receber status CONFIRMED.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatusBookingResposta> consultarStatus(@PathVariable UUID id) {
        Booking booking = bookingService.buscarPorIdOuFalhar(id);
        return ResponseEntity.ok(StatusBookingResposta.de(booking));
    }
}