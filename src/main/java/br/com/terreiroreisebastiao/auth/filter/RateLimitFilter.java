package br.com.terreiroreisebastiao.auth.filter;

import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import br.com.terreiroreisebastiao.shared.error.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Filtro de rate limiting para endpoints de autenticação.
 *
 * <p>Implementa token bucket simples com {@link ConcurrentHashMap}, limitando
 * a 10 requisições por minuto por IP em {@code /api/v1/auth/*}.</p>
 *
 * <p>Retorna {@code 429 Too Many Requests} com header {@code Retry-After}.</p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE_POR_MINUTO = 10;
    private static final long JANELA_MILLIS = 60_000L;
    private static final String PREFIXO_AUTH = "/api/v1/auth/";

    private final Map<String, BucketSimples> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String caminho = request.getRequestURI();

        if (!caminho.startsWith(PREFIXO_AUTH)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extrairIp(request);
        BucketSimples bucket = buckets.computeIfAbsent(ip, k -> new BucketSimples());

        if (!bucket.tentarConsumir()) {
            long segundosRestantes = bucket.segundosAteReset();
            responderComRateLimited(request, response, segundosRestantes);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extrairIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void responderComRateLimited(HttpServletRequest request,
                                         HttpServletResponse response,
                                         long segundosRestantes) throws IOException {
        String traceId = MDC.get("traceId");
        if (traceId == null) traceId = UUID.randomUUID().toString();

        ProblemDetails problema = ProblemDetails.of(
                ErrorCode.RATE_LIMITED,
                "Muitas tentativas. Aguarde antes de tentar novamente.",
                request.getRequestURI(), traceId
        );

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(segundosRestantes));
        objectMapper.writeValue(response.getOutputStream(), problema);
    }

    /**
     * Token bucket simples com janela fixa de 1 minuto.
     */
    private static class BucketSimples {

        private final AtomicInteger contagem = new AtomicInteger(0);
        private final AtomicLong inicioJanela = new AtomicLong(System.currentTimeMillis());

        boolean tentarConsumir() {
            long agora = System.currentTimeMillis();
            long inicio = inicioJanela.get();

            if (agora - inicio > JANELA_MILLIS) {
                inicioJanela.set(agora);
                contagem.set(1);
                return true;
            }

            return contagem.incrementAndGet() <= LIMITE_POR_MINUTO;
        }

        long segundosAteReset() {
            long restante = JANELA_MILLIS - (System.currentTimeMillis() - inicioJanela.get());
            return Math.max(1, restante / 1000);
        }
    }
}
