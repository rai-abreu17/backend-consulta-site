package br.com.terreiroreisebastiao.payment.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Component
public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);

    @Value("${mp.webhook.secret}")
    private String secret;

    @PostConstruct
    void validarConfiguracaoSegredo() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                "Configuração ausente: 'mp.webhook.secret' deve ser definida no ambiente de execução. " +
                "O serviço de verificação de assinatura de webhook não pode inicializar sem este segredo."
            );
        }
    }

    /**
     * Valida a assinatura HMAC enviada pelo Mercado Pago no header x-signature.
     *
     * @param signatureHeader Valor bruto do header x-signature (ex: "ts=...,v1=...")
     * @param requestId Header x-request-id enviado pelo Mercado Pago
     * @param resourceId Identificador do recurso do pagamento enviado no payload
     * @return true se a assinatura for válida
     */
    public boolean isSignatureValid(String signatureHeader, String requestId, String resourceId) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(signatureHeader)) {
            return false;
        }

        try {
            Map<String, String> partes = parseSignature(signatureHeader);
            String timestamp = partes.get("ts");
            String assinaturaInformada = partes.get("v1");
            if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(assinaturaInformada)) {
                return false;
            }

            String manifest = "id:" + valorSeguro(resourceId)
                    + ";request-id:" + valorSeguro(requestId)
                    + ";ts:" + timestamp
                    + ";";

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            String assinaturaEsperada = HexFormat.of().formatHex(hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));

            boolean valida = MessageDigest.isEqual(
                    assinaturaEsperada.getBytes(StandardCharsets.UTF_8),
                    assinaturaInformada.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
            );

            if (!valida) {
                log.warn("Assinatura do webhook inválida. requestId={} resourceId={}", requestId, resourceId);
            }

            return valida;
        } catch (Exception e) {
            log.error("Falha ao validar assinatura do webhook. requestId={} motivo={}", requestId, e.getMessage());
            return false;
        }
    }

    private Map<String, String> parseSignature(String signatureHeader) {
        Map<String, String> partes = new HashMap<>();
        for (String fragmento : signatureHeader.split(",")) {
            String[] chaveValor = fragmento.trim().split("=", 2);
            if (chaveValor.length == 2 && StringUtils.hasText(chaveValor[0]) && StringUtils.hasText(chaveValor[1])) {
                partes.put(chaveValor[0].trim().toLowerCase(Locale.ROOT), chaveValor[1].trim());
            }
        }
        return partes;
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor;
    }
}
