package br.com.terreiroreisebastiao.auth.filter;

import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import br.com.terreiroreisebastiao.shared.error.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Filtro CSRF baseado em Double-Submit Cookie.
 *
 * <p>Para métodos de mutação (POST, PUT, PATCH, DELETE), valida que o header
 * {@code X-CSRF-Token} corresponde ao valor do cookie {@code csrf_token}.</p>
 *
 * <p>Endpoints isentos de CSRF: login, refresh e webhooks.</p>
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    private static final String COOKIE_CSRF = "csrf_token";
    private static final String HEADER_CSRF = "X-CSRF-Token";
    private static final Set<String> METODOS_LEITURA = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final Set<String> CAMINHOS_ISENTOS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/payments/webhooks/mercado-pago"
    );

    private final ObjectMapper objectMapper;

    public CsrfCookieFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (METODOS_LEITURA.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String caminho = request.getRequestURI();
        if (CAMINHOS_ISENTOS.contains(caminho)) {
            filterChain.doFilter(request, response);
            return;
        }

        String csrfCookie = extrairCsrfDoCookie(request);
        String csrfHeader = request.getHeader(HEADER_CSRF);

        if (csrfCookie == null || csrfHeader == null || !csrfCookie.equals(csrfHeader)) {
            responderComErroCsrf(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extrairCsrfDoCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (COOKIE_CSRF.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void responderComErroCsrf(HttpServletRequest request,
                                      HttpServletResponse response) throws IOException {
        String traceId = MDC.get("traceId");
        if (traceId == null) traceId = UUID.randomUUID().toString();

        ProblemDetails problema = ProblemDetails.of(
                ErrorCode.CSRF_TOKEN_MISMATCH,
                "Token CSRF ausente ou inválido. Recarregue a página e tente novamente.",
                request.getRequestURI(), traceId
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
