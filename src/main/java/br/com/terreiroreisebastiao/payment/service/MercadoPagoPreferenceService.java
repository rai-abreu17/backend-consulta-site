package br.com.terreiroreisebastiao.payment.service;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.payment.domain.Pagamento;
import br.com.terreiroreisebastiao.payment.domain.PaymentStatus;
import br.com.terreiroreisebastiao.payment.repository.PagamentoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Integração com a Preferences API do Mercado Pago.
 *
 * <p>Quando {@code mp.access-token} está vazio (ambiente de dev local), retorna
 * uma URL de mock para não quebrar o fluxo. Em Staging/Produção o token deve
 * estar configurado e a chamada real é feita.</p>
 */
@Service
public class MercadoPagoPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPreferenceService.class);
    private static final String MP_PREFERENCES_URL = "https://api.mercadopago.com/checkout/preferences";

    @Value("${mp.access-token}")
    private String accessToken;

    private final PagamentoRepository pagamentoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public MercadoPagoPreferenceService(PagamentoRepository pagamentoRepository) {
        this.pagamentoRepository = pagamentoRepository;
    }

    /**
     * Cria uma preferência de pagamento no Mercado Pago e persiste o registro
     * {@link Pagamento} com a URL de checkout gerada.
     *
     * @param booking o agendamento em PENDING_PAYMENT
     * @return URL de checkout ({@code init_point} ou {@code sandbox_init_point})
     */
    @Transactional
    public String criarPreferenciaEObterUrl(Booking booking) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("mp.access-token não configurado — retornando URL de mock. bookingId={}", booking.getId());
            String mockUrl = "https://sandbox.mercadopago.com.br/mock/checkout?bookingId=" + booking.getId();
            persistirPagamento(booking, "mock-pref-" + booking.getId(), mockUrl, null);
            return mockUrl;
        }

        try {
            String payload = construirPayload(booking);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MP_PREFERENCES_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Erro na API do Mercado Pago. status={} body={}", response.statusCode(), response.body());
                throw new RuntimeException("Mercado Pago retornou HTTP " + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String prefId = json.path("id").asText();

            // Sandbox expõe sandbox_init_point; produção expõe init_point
            String checkoutUrl = (json.has("sandbox_init_point") && !json.get("sandbox_init_point").asText().isBlank())
                    ? json.get("sandbox_init_point").asText()
                    : json.path("init_point").asText();

            log.info("Preferência MP criada. bookingId={} prefId={} url={}", booking.getId(), prefId, checkoutUrl);
            persistirPagamento(booking, prefId, checkoutUrl, response.body());
            return checkoutUrl;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrompida durante chamada ao Mercado Pago. bookingId={}", booking.getId(), e);
            throw new RuntimeException("Requisição ao Mercado Pago foi interrompida.", e);
        } catch (Exception e) {
            log.error("Falha ao criar preferência no Mercado Pago. bookingId={}", booking.getId(), e);
            throw new RuntimeException("Falha ao criar preferência de pagamento: " + e.getMessage(), e);
        }
    }

    private String construirPayload(Booking booking) throws Exception {
        BigDecimal precoBrl = BigDecimal.valueOf(booking.getPrecoCents())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        ObjectNode item = objectMapper.createObjectNode();
        item.put("title", "Consulta – " + booking.getServico().getNome());
        item.put("quantity", 1);
        item.put("currency_id", "BRL");
        item.put("unit_price", precoBrl);

        ArrayNode items = objectMapper.createArrayNode();
        items.add(item);

        ObjectNode root = objectMapper.createObjectNode();
        root.set("items", items);
        root.put("external_reference", booking.getId().toString());

        return objectMapper.writeValueAsString(root);
    }

    private void persistirPagamento(Booking booking, String prefId, String checkoutUrl, String respostaBruta) {
        if (pagamentoRepository.findByAgendamentoId(booking.getId()).isPresent()) {
            log.warn("Pagamento já existe para bookingId={}. Ignorando duplicata.", booking.getId());
            return;
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setAgendamento(booking);
        pagamento.setStatus(PaymentStatus.CREATED);
        pagamento.setIdPreferenciaProvedor(prefId);
        pagamento.setUrlCheckout(checkoutUrl);
        pagamento.setValorCents(booking.getPrecoCents());
        pagamento.setRespostaBruta(respostaBruta);

        pagamentoRepository.save(pagamento);
        log.info("Entidade Pagamento persistida. bookingId={}", booking.getId());
    }
}
