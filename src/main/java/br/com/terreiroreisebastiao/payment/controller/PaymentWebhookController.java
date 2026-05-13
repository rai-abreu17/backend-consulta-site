package br.com.terreiroreisebastiao.payment.controller;

import br.com.terreiroreisebastiao.payment.service.PaymentProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller blindado para processamento de Webhooks do Mercado Pago.
 * Não utiliza blocos try-catch e não expõe Entidades JPA.
 */
@RestController
@RequestMapping({"/api/v1/public/webhooks", "/api/v1/payments/webhooks"})
public class PaymentWebhookController {

    private final PaymentProcessingService paymentProcessingService;

    public PaymentWebhookController(PaymentProcessingService paymentProcessingService) {
        this.paymentProcessingService = paymentProcessingService;
    }

    /**
     * Recepção do webhook (endpoint público do provedor).
     *
     * @param signature Assinatura enviada no header x-signature
     * @param requestId Header opcional x-request-id
     * @param payload Payload completo do evento no formato map/json
     * @return 200 OK (ACK) imediato, independentemente do sucesso do processamento assíncrono.
     */
    @PostMapping({"/mercadopago", "/mercado-pago"})
    public ResponseEntity<WebhookResponse> receiveWebhook(
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestBody String rawPayload) {
        WebhookResponse response = paymentProcessingService.processWebhookEvent(signature, requestId, rawPayload);
        return ResponseEntity.ok(response);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    public record WebhookResponse(
            boolean received,
            String eventId,
            boolean deduplicated
    ) {}
}
