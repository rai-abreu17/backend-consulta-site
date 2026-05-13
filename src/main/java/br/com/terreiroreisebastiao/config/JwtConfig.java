package br.com.terreiroreisebastiao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propriedades de configuração do JWT, injetadas via {@code application.yml}.
 *
 * <pre>
 * jwt:
 *   secret: ${JWT_SECRET}
 *   expiration: 24h
 *   refresh-ttl-days: 30
 * </pre>
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
        String secret,
        Duration expiration,
        int refreshTtlDays
) {
    /** Valores padrão para TTLs quando não especificados. */
    public JwtConfig {
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            expiration = Duration.ofHours(24);
        }
        if (refreshTtlDays <= 0) refreshTtlDays = 30;
    }
}
