package br.com.terreiroreisebastiao.auth.controller;

import br.com.terreiroreisebastiao.auth.dto.RequisicaoLogin;
import br.com.terreiroreisebastiao.auth.dto.RespostaUsuario;
import br.com.terreiroreisebastiao.auth.service.AuthService;
import br.com.terreiroreisebastiao.auth.service.AuthService.ResultadoAutenticacao;
import br.com.terreiroreisebastiao.auth.service.CookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller REST para autenticação, sessão e consulta do administrador.
 *
 * <p>Os tokens nunca aparecem no body de resposta — trafegam exclusivamente
 * via cookies {@code Set-Cookie} nos headers HTTP.</p>
 *
 * @see AuthService
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    public AuthController(AuthService authService, CookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    /**
     * {@code POST /api/v1/auth/login} — Autentica o administrador.
     *
     * <p>Retorna dados do usuário no body e tokens nos cookies Set-Cookie.</p>
     */
    @PostMapping("/login")
    public ResponseEntity<RespostaUsuario> login(
            @Valid @RequestBody RequisicaoLogin requisicao,
            HttpServletRequest request) {

        ResultadoAutenticacao resultado = authService.autenticar(
                requisicao.email(),
                requisicao.senha(),
                request.getHeader("User-Agent"),
                extrairIp(request)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resultado.cookieAccess().toString())
                .header(HttpHeaders.SET_COOKIE, resultado.cookieRefresh().toString())
                .header(HttpHeaders.SET_COOKIE, resultado.cookieCsrf().toString())
                .body(resultado.respostaUsuario());
    }

    /**
     * {@code POST /api/v1/auth/refresh} — Renova os tokens de sessão.
     *
     * <p>Lê o refresh token do cookie HttpOnly, rotaciona e emite novos cookies.</p>
     */
    @PostMapping("/refresh")
    public ResponseEntity<RespostaUsuario> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request) {

        ResultadoAutenticacao resultado = authService.renovarToken(
                refreshToken,
                request.getHeader("User-Agent"),
                extrairIp(request)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resultado.cookieAccess().toString())
                .header(HttpHeaders.SET_COOKIE, resultado.cookieRefresh().toString())
                .header(HttpHeaders.SET_COOKIE, resultado.cookieCsrf().toString())
                .body(resultado.respostaUsuario());
    }

    /**
     * {@code POST /api/v1/auth/logout} — Encerra a sessão.
     *
     * <p>Revoga o refresh token e limpa todos os cookies de autenticação.</p>
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        authService.encerrarSessao(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.limparCookieAccessToken().toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.limparCookieRefreshToken().toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.limparCookieCsrf().toString())
                .build();
    }

    /**
     * {@code GET /api/v1/auth/me} — Retorna dados do administrador autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<RespostaUsuario> me(Authentication authentication) {
        UUID adminId = (UUID) authentication.getPrincipal();
        RespostaUsuario resposta = authService.consultarUsuarioAutenticado(adminId);
        return ResponseEntity.ok(resposta);
    }

    private String extrairIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
