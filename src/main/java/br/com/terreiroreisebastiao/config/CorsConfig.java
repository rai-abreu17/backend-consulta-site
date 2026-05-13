package br.com.terreiroreisebastiao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração de CORS para o backend.
 *
 * <p>Em ambiente local, permite origens do Vite dev server.
 * {@code allowCredentials=true} é necessário para cookies HttpOnly.</p>
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173}")
    private String origensPermitidas;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(Arrays.stream(origensPermitidas.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList());
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("*"));
        configuracao.setExposedHeaders(List.of("X-Token-Expired", "Retry-After"));
        configuracao.setAllowCredentials(true);
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuracao);
        return source;
    }
}
