package br.com.terreiroreisebastiao.payment.service;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import br.com.terreiroreisebastiao.booking.repository.BookingRepository;
import br.com.terreiroreisebastiao.notification.service.NotificationService;
import br.com.terreiroreisebastiao.payment.controller.PaymentWebhookController.WebhookResponse;
import br.com.terreiroreisebastiao.payment.domain.EventoWebhookPagamento;
import br.com.terreiroreisebastiao.payment.domain.Pagamento;
import br.com.terreiroreisebastiao.payment.domain.PaymentStatus;
import br.com.terreiroreisebastiao.payment.repository.EventoWebhookPagamentoRepository;
import br.com.terreiroreisebastiao.payment.repository.PagamentoRepository;
import br.com.terreiroreisebastiao.shared.error.ApiException;
import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import br.com.terreiroreisebastiao.shared.lock.LockService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingService.class);
    private static final String PROVEDOR = "MERCADO_PAGO";
    private static final long LOCK_WAIT_SECONDS = 3L;

    private final WebhookSignatureVerifier signatureVerifier;
    private final EventoWebhookPagamentoRepository eventoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final LockService lockService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String mercadoPagoAccessToken;
    private final String mercadoPagoBaseUrl;
    private final PaymentProcessingService self;

    public PaymentProcessingService(WebhookSignatureVerifier signatureVerifier,
                                    EventoWebhookPagamentoRepository eventoRepository,
                                    PagamentoRepository pagamentoRepository,
                                    BookingRepository bookingRepository,
                                    NotificationService notificationService,
                                    LockService lockService,
                                    ObjectMapper objectMapper,
                                    @Value("${mp.access-token:}") String mercadoPagoAccessToken,
                                    @Value("${mp.base-url:https://api.mercadopago.com}") String mercadoPagoBaseUrl,
                                    @Lazy PaymentProcessingService self) {
        this.signatureVerifier = signatureVerifier;
        this.eventoRepository = eventoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.lockService = lockService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mercadoPagoAccessToken = mercadoPagoAccessToken;
        this.mercadoPagoBaseUrl = mercadoPagoBaseUrl;
        this.self = self;
    }

    public WebhookResponse processWebhookEvent(String signature, String requestId, String rawPayload) {
        JsonNode payload = parsePayload(rawPayload);
        WebhookRecebido webhook = extrairWebhook(payload, requestId);

        if (!signatureVerifier.isSignatureValid(signature, requestId, webhook.resourceId())) {
            throw new ApiException(ErrorCode.PAYMENT_WEBHOOK_INVALID_SIGNATURE, "Assinatura do webhook inválida.");
        }

        EventoWebhookPagamento evento = persistirEventoRecebido(webhook, signature, rawPayload);
        if (evento == null) {
            log.info("Webhook Mercado Pago deduplicado. eventId={}", webhook.eventId());
            return new WebhookResponse(true, webhook.eventId(), true);
        }

        try {
            self.processarEventoPersistido(evento.getId(), rawPayload);
            return new WebhookResponse(true, webhook.eventId(), false);
        } catch (ApiException ex) {
            self.registrarFalha(evento.getId(), resumirErro(ex));
            throw ex;
        } catch (RuntimeException ex) {
            self.registrarFalha(evento.getId(), resumirErro(ex));
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao processar webhook de pagamento."
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processarEventoPersistido(UUID eventoId, String rawPayload) {
        EventoWebhookPagamento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Evento de webhook não encontrado."));

        JsonNode payload = parsePayload(rawPayload);
        JsonNode dadosPagamento = resolverPayloadAutoritativo(evento.getIdRecurso(), payload);
        PaymentStatus statusPagamento = resolverStatusPagamento(dadosPagamento, payload);
        Pagamento pagamento = localizarPagamento(evento.getIdRecurso(), dadosPagamento, payload);
        if (pagamento == null) {
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Pagamento do webhook ainda não está disponível para processamento."
            );
        }

        UUID bookingId = pagamento.getAgendamento().getId();
        String lockKey = "booking:" + bookingId;

        log.info("Tentando adquirir lock do webhook. eventId={} bookingId={} chave={}", evento.getIdEvento(), bookingId, lockKey);

        boolean locked = lockService.executeWithLock(lockKey, LOCK_WAIT_SECONDS, () -> {
            log.info("Lock do webhook adquirido. eventId={} bookingId={} chave={}", evento.getIdEvento(), bookingId, lockKey);

            Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Agendamento do pagamento não encontrado."));
            BookingStatus statusAnterior = booking.getStatus();

            aplicarTransicaoDePagamento(pagamento, booking, statusPagamento, dadosPagamento);

            if (statusAnterior != BookingStatus.CONFIRMED && booking.getStatus() == BookingStatus.CONFIRMED) {
                notificationService.agendarConfirmacao(booking);
            }

            pagamentoRepository.save(pagamento);
            bookingRepository.save(booking);

            evento.setErroProcessamento(null);
            evento.setProcessadoEm(OffsetDateTime.now(ZoneOffset.UTC));
            eventoRepository.save(evento);

            log.info("Webhook Mercado Pago processado. eventId={} bookingId={} paymentStatus={} bookingStatus={}",
                    evento.getIdEvento(), bookingId, pagamento.getStatus(), booking.getStatus());

            log.info("Lock do webhook liberado. eventId={} bookingId={} chave={}", evento.getIdEvento(), bookingId, lockKey);
        });

        if (!locked) {
            throw new ApiException(
                    ErrorCode.EXTERNAL_TIMEOUT,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível sincronizar o processamento do webhook."
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalha(UUID eventoId, String erro) {
        eventoRepository.findById(eventoId).ifPresent(evento -> {
            evento.incrementaTentativas();
            evento.setErroProcessamento(limitarMensagem(erro));
            eventoRepository.save(evento);
        });
    }

    private EventoWebhookPagamento persistirEventoRecebido(WebhookRecebido webhook, String signature, String rawPayload) {
        try {
            EventoWebhookPagamento evento = new EventoWebhookPagamento(
                    webhook.eventId(),
                    webhook.eventType(),
                    webhook.resourceId(),
                    signature,
                    rawPayload
            );
            return eventoRepository.saveAndFlush(evento);
        } catch (DataIntegrityViolationException ex) {
            if (eventoRepository.findByProvedorAndIdEvento(PROVEDOR, webhook.eventId()).isPresent()) {
                return null;
            }
            throw ex;
        }
    }

    private JsonNode parsePayload(String rawPayload) {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Payload do webhook em formato inválido.");
        }
    }

    private WebhookRecebido extrairWebhook(JsonNode payload, String requestId) {
        String eventType = primeiroTexto(
                payload.path("type").asText(null),
                payload.path("action").asText(null)
        );
        String resourceId = primeiroTexto(
                payload.path("data").path("id").asText(null),
                payload.path("resource").path("id").asText(null),
                payload.path("id").asText(null)
        );
        String eventId = StringUtils.hasText(requestId)
                ? requestId
                : primeiroTexto(payload.path("id").asText(null), resourceId + ":" + eventType);

        if (!StringUtils.hasText(eventType) || !StringUtils.hasText(resourceId) || !StringUtils.hasText(eventId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Payload do webhook sem os identificadores obrigatórios.");
        }

        return new WebhookRecebido(eventId, eventType, resourceId);
    }

    private JsonNode resolverPayloadAutoritativo(String resourceId, JsonNode payloadOriginal) {
        if (!StringUtils.hasText(resourceId) || !StringUtils.hasText(mercadoPagoAccessToken)) {
            return payloadOriginal;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mercadoPagoBaseUrl + "/v1/payments/" + resourceId))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + mercadoPagoAccessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readTree(response.body());
            }

            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Mercado Pago respondeu com status inesperado durante a consulta do pagamento."
            );
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao consultar o pagamento no Mercado Pago."
            );
        }
    }

    private Pagamento localizarPagamento(String resourceId, JsonNode payloadAutoritativo, JsonNode payloadOriginal) {
        if (StringUtils.hasText(resourceId)) {
            Pagamento pagamento = pagamentoRepository.findByIdPagamentoProvedor(resourceId).orElse(null);
            if (pagamento != null) {
                return pagamento;
            }
        }

        UUID bookingId = extrairBookingId(payloadAutoritativo);
        if (bookingId == null) {
            bookingId = extrairBookingId(payloadOriginal);
        }
        if (bookingId != null) {
            return pagamentoRepository.findByAgendamentoId(bookingId).orElse(null);
        }

        return null;
    }

    private PaymentStatus resolverStatusPagamento(JsonNode payloadAutoritativo, JsonNode payloadOriginal) {
        String status = primeiroTexto(
                payloadAutoritativo.path("status").asText(null),
                payloadAutoritativo.path("data").path("status").asText(null),
                payloadOriginal.path("status").asText(null),
                payloadOriginal.path("data").path("status").asText(null)
        );

        if (!StringUtils.hasText(status)) {
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "O status do pagamento não pôde ser determinado a partir do webhook."
            );
        }

        return mapearStatus(status);
    }

    private void aplicarTransicaoDePagamento(Pagamento pagamento,
                                             Booking booking,
                                             PaymentStatus statusPagamento,
                                             JsonNode payloadAutoritativo) {
        switch (statusPagamento) {
            case APPROVED -> {
                pagamento.setStatus(PaymentStatus.APPROVED);
                pagamento.setAprovadoEm(extrairData(payloadAutoritativo, "date_approved", OffsetDateTime.now(ZoneOffset.UTC)));
                if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                    booking.setStatus(BookingStatus.CONFIRMED);
                    booking.setExpiracaoReservaEm(null);
                }
            }
            case REJECTED, CANCELLED -> {
                pagamento.setStatus(statusPagamento);
                if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                    booking.setStatus(BookingStatus.CANCELLED);
                }
            }
            case REFUNDED -> {
                pagamento.setStatus(PaymentStatus.REFUNDED);
                pagamento.setEstornadoEm(extrairData(payloadAutoritativo, "date_last_updated", OffsetDateTime.now(ZoneOffset.UTC)));
                booking.setStatus(BookingStatus.REFUNDED);
                booking.setExpiracaoReservaEm(null);
            }
            case PENDING -> pagamento.setStatus(PaymentStatus.PENDING);
            default -> throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Status de pagamento não suportado para processamento."
            );
        }
    }

    private PaymentStatus mapearStatus(String status) {
        String normalizado = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalizado) {
            case "APPROVED" -> PaymentStatus.APPROVED;
            case "REJECTED" -> PaymentStatus.REJECTED;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            case "REFUNDED" -> PaymentStatus.REFUNDED;
            case "IN_PROCESS", "PENDING" -> PaymentStatus.PENDING;
            default -> throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Status recebido do Mercado Pago não é suportado pelo backend."
            );
        };
    }

    private UUID extrairBookingId(JsonNode payload) {
        return parseUuid(primeiroTexto(
                payload.path("metadata").path("bookingId").asText(null),
                payload.path("metadata").path("booking_id").asText(null),
                payload.path("external_reference").asText(null),
                payload.path("bookingId").asText(null),
                payload.path("data").path("metadata").path("bookingId").asText(null),
                payload.path("data").path("external_reference").asText(null)
        ));
    }

    private OffsetDateTime extrairData(JsonNode payload, String campo, OffsetDateTime padrao) {
        String valor = payload.path(campo).asText(null);
        if (!StringUtils.hasText(valor)) {
            return padrao;
        }
        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException ex) {
            return padrao;
        }
    }

    private UUID parseUuid(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String primeiroTexto(String... candidatos) {
        for (String candidato : candidatos) {
            if (StringUtils.hasText(candidato)) {
                return candidato;
            }
        }
        return null;
    }

    private String resumirErro(Throwable erro) {
        if (erro == null) {
            return "Falha não identificada.";
        }
        String mensagem = erro.getMessage();
        if (!StringUtils.hasText(mensagem)) {
            mensagem = erro.getClass().getSimpleName();
        }
        return limitarMensagem(mensagem);
    }

    private String limitarMensagem(String mensagem) {
        if (!StringUtils.hasText(mensagem)) {
            return "Falha não identificada.";
        }
        return mensagem.length() <= 500 ? mensagem : mensagem.substring(0, 500);
    }

    private record WebhookRecebido(String eventId, String eventType, String resourceId) {}
}
