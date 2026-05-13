package br.com.terreiroreisebastiao.auth.service;

import br.com.terreiroreisebastiao.config.JwtConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Serviço responsável pela criação e limpeza de cookies de autenticação.
 *
 * <p>Gere três cookies conforme SPEC (F-002-01):
 * <ul>
 *   <li>{@code access_token}: JWT, HttpOnly, Secure, SameSite=Strict, Path=/api/v1</li>
 *   <li>{@code refresh_token}: Opaque, HttpOnly, Secure, SameSite=Strict, Path=/api/v1/auth/refresh</li>
 *   <li>{@code csrf_token}: NÃO HttpOnly, Secure, SameSite=Strict, Path=/</li>
 * </ul>
 */
@Component
public class CookieService {

    private static final String COOKIE_ACCESS = "access_token";
    private static final String COOKIE_REFRESH = "refresh_token";
    private static final String COOKIE_CSRF = "csrf_token";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final int tempoAccessSegundos;
    private final int tempoRefreshSegundos;
    private final boolean seguro;

    public CookieService(JwtConfig config,
                         @Value("${server.ssl.enabled:true}") boolean seguro) {
        this.tempoAccessSegundos = Math.toIntExact(config.expiration().getSeconds());
        this.tempoRefreshSegundos = config.refreshTtlDays() * 86400;
        this.seguro = seguro;
    }

    /** Cria cookie HttpOnly para o access token (JWT). */
    public ResponseCookie criarCookieAccessToken(String token) {
        return ResponseCookie.from(COOKIE_ACCESS, token)
                .httpOnly(true)
                .secure(seguro)
                .sameSite("Strict")
                .path("/api/v1")
                .maxAge(Duration.ofSeconds(tempoAccessSegundos))
                .build();
    }

    /** Cria cookie HttpOnly para o refresh token (opaque). */
    public ResponseCookie criarCookieRefreshToken(String token) {
        return ResponseCookie.from(COOKIE_REFRESH, token)
                .httpOnly(true)
                .secure(seguro)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofSeconds(tempoRefreshSegundos))
                .build();
    }

    /** Cria cookie NÃO HttpOnly para o CSRF token (lido pelo frontend). */
    public ResponseCookie criarCookieCsrf(String token) {
        return ResponseCookie.from(COOKIE_CSRF, token)
                .httpOnly(false)
                .secure(seguro)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofSeconds(tempoAccessSegundos))
                .build();
    }

    /** Cria cookies de limpeza (Max-Age=0) para logout. */
    public ResponseCookie limparCookieAccessToken() {
        return ResponseCookie.from(COOKIE_ACCESS, "").path("/api/v1").maxAge(0).build();
    }

    public ResponseCookie limparCookieRefreshToken() {
        return ResponseCookie.from(COOKIE_REFRESH, "").path("/api/v1/auth/refresh").maxAge(0).build();
    }

    public ResponseCookie limparCookieCsrf() {
        return ResponseCookie.from(COOKIE_CSRF, "").path("/").maxAge(0).build();
    }

    /** Gera um refresh token opaque de 32 bytes aleatórios, codificado em hex. */
    public String gerarRefreshTokenOpaque() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Gera um CSRF token aleatório de 24 bytes, codificado em Base64 URL-safe. */
    public String gerarCsrfToken() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
