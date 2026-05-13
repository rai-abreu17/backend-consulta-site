package br.com.terreiroreisebastiao.shared.error;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Handler global de exceções — transversal a todos os controllers.
 *
 * <p>Converte todas as exceções em {@link ProblemDetails} (RFC 7807).
 * Stack traces nunca chegam ao cliente — apenas ao log estruturado.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    private final MeterRegistry meterRegistry;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ── Exceções de domínio ─────────────────────────────────

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetails> tratarExcecaoDeDominio(ApiException ex,
                                                                 HttpServletRequest request) {
        String traceId = obterTraceId();
        String instance = request.getRequestURI();

        log.warn("Exceção de domínio. code={} status={} traceId={}",
                ex.getCode(), ex.getStatus().value(), traceId);

        incrementarContador(ex.getCode());

        ProblemDetails body = ex.getFieldErrors().isEmpty()
                ? ProblemDetails.of(ex.getCode(), ex.getStatus().value(), ex.getMessage(), instance, traceId)
                : ProblemDetails.comErrosDeCampo(ex.getCode(), ex.getMessage(), instance, traceId, ex.getFieldErrors());

        ResponseEntity.BodyBuilder builder = ResponseEntity
                .status(ex.getStatus())
                .contentType(PROBLEM_JSON);

        if (ex.getCode() == ErrorCode.ACCOUNT_LOCKED) {
            builder.header("Retry-After", "900");
        }
        if (ex.getCode() == ErrorCode.RATE_LIMITED) {
            builder.header("Retry-After", "60");
        }

        return builder.body(body);
    }

    // ── Validação (Bean Validation / Jakarta) ───────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> tratarValidacaoDeArgumentos(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = obterTraceId();
        incrementarContador(ErrorCode.VALIDATION_ERROR);

        List<FieldError> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ProblemDetails body = ProblemDetails.comErrosDeCampo(
                ErrorCode.VALIDATION_ERROR,
                "Um ou mais campos contêm valores inválidos.",
                request.getRequestURI(), traceId, erros
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetails> tratarConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String traceId = obterTraceId();
        incrementarContador(ErrorCode.VALIDATION_ERROR);

        List<FieldError> erros = ex.getConstraintViolations().stream()
                .map(cv -> new FieldError(
                        cv.getPropertyPath().toString(),
                        cv.getMessage()))
                .toList();

        ProblemDetails body = ProblemDetails.comErrosDeCampo(
                ErrorCode.VALIDATION_ERROR,
                "Um ou mais campos contêm valores inválidos.",
                request.getRequestURI(), traceId, erros
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetails> tratarMensagemIlegivel(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String traceId = obterTraceId();
        incrementarContador(ErrorCode.VALIDATION_ERROR);

        ProblemDetails body = ProblemDetails.of(
                ErrorCode.VALIDATION_ERROR,
                "Corpo da requisição ausente ou em formato inválido.",
                request.getRequestURI(), traceId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetails> tratarTipoDeArgumento(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String traceId = obterTraceId();
        incrementarContador(ErrorCode.VALIDATION_ERROR);

        ProblemDetails body = ProblemDetails.of(
                ErrorCode.VALIDATION_ERROR,
                "Parâmetro '" + ex.getName() + "' contém valor inválido.",
                request.getRequestURI(), traceId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM_JSON).body(body);
    }

    // ── Spring Security ─────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetails> tratarAcessoNegado(
            AccessDeniedException ex, HttpServletRequest request) {
        String traceId = obterTraceId();
        incrementarContador(ErrorCode.FORBIDDEN);

        ProblemDetails body = ProblemDetails.of(
                ErrorCode.FORBIDDEN,
                "Você não tem permissão para acessar este recurso.",
                request.getRequestURI(), traceId
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(PROBLEM_JSON).body(body);
    }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ProblemDetails> tratarCredenciaisInvalidas(
                        BadCredentialsException ex, HttpServletRequest request) {
                String traceId = obterTraceId();
                incrementarContador(ErrorCode.INVALID_CREDENTIALS);

                ProblemDetails body = ProblemDetails.of(
                                ErrorCode.INVALID_CREDENTIALS,
                                "E-mail ou senha incorretos.",
                                request.getRequestURI(), traceId
                );

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(PROBLEM_JSON).body(body);
        }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetails> tratarFalhaDeAutenticacao(
            AuthenticationException ex, HttpServletRequest request) {
        String traceId = obterTraceId();
        incrementarContador(ErrorCode.UNAUTHENTICATED);

        ProblemDetails body = ProblemDetails.of(
                ErrorCode.UNAUTHENTICATED,
                "Autenticação necessária para acessar este recurso.",
                request.getRequestURI(), traceId
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(PROBLEM_JSON).body(body);
    }

        @ExceptionHandler(ExpiredJwtException.class)
        public ResponseEntity<ProblemDetails> tratarJwtExpirado(
                        ExpiredJwtException ex, HttpServletRequest request) {
                String traceId = obterTraceId();
                incrementarContador(ErrorCode.TOKEN_EXPIRED);

                ProblemDetails body = ProblemDetails.of(
                                ErrorCode.TOKEN_EXPIRED,
                                "O token JWT expirou. Faça login novamente.",
                                request.getRequestURI(), traceId
                );

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(PROBLEM_JSON).body(body);
        }

        @ExceptionHandler(JwtException.class)
        public ResponseEntity<ProblemDetails> tratarJwtInvalido(
                        JwtException ex, HttpServletRequest request) {
                String traceId = obterTraceId();
                incrementarContador(ErrorCode.TOKEN_INVALID);

                ProblemDetails body = ProblemDetails.of(
                                ErrorCode.TOKEN_INVALID,
                                "O token JWT informado é inválido.",
                                request.getRequestURI(), traceId
                );

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(PROBLEM_JSON).body(body);
        }

    // ── Recurso não encontrado ──────────────────────────────

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetails> tratarRecursoNaoEncontrado(
            NoResourceFoundException ex, HttpServletRequest request) {
        String traceId = obterTraceId();
        incrementarContador(ErrorCode.RESOURCE_NOT_FOUND);

        ProblemDetails body = ProblemDetails.of(
                ErrorCode.RESOURCE_NOT_FOUND,
                "O recurso solicitado não foi encontrado.",
                request.getRequestURI(), traceId
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(PROBLEM_JSON).body(body);
    }

    // ── Integridade de dados (PostgreSQL) ───────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetails> tratarViolacaoDeIntegridade(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String traceId = obterTraceId();

        ErrorCode codigo = mapearCodigoPostgres(ex);
        incrementarContador(codigo);

        ProblemDetails body = ProblemDetails.of(
                codigo,
                codigo == ErrorCode.SLOT_ALREADY_TAKEN
                        ? "O horário selecionado já está reservado."
                        : "Violação de integridade de dados. Verifique os campos informados.",
                request.getRequestURI(), traceId
        );

        return ResponseEntity.status(codigo.getDefaultStatus()).contentType(PROBLEM_JSON).body(body);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetails> tratarFalhaDeLockPessimista(
            PessimisticLockingFailureException ex, HttpServletRequest request) {
        String traceId = obterTraceId();
        incrementarContador(ErrorCode.SLOT_ALREADY_TAKEN);

        ProblemDetails body = ProblemDetails.of(
                ErrorCode.SLOT_ALREADY_TAKEN,
                "O horário selecionado foi reservado por outra operação simultânea.",
                request.getRequestURI(), traceId
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(PROBLEM_JSON).body(body);
    }

    // ── Timeout externo ─────────────────────────────────────

    @ExceptionHandler(TransactionTimedOutException.class)
    public ResponseEntity<ProblemDetails> tratarTimeoutDeTransacao(
            TransactionTimedOutException ex, HttpServletRequest request) {
        String traceId = obterTraceId();
        incrementarContador(ErrorCode.EXTERNAL_TIMEOUT);
        log.error("Timeout de transação. traceId={}", traceId, ex);

        ProblemDetails body = ProblemDetails.of(
                ErrorCode.EXTERNAL_TIMEOUT,
                "A operação demorou mais do que o esperado. Tente novamente.",
                request.getRequestURI(), traceId
        );

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).contentType(PROBLEM_JSON).body(body);
    }

    // ── Fallback genérico ───────────────────────────────────

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ProblemDetails> tratarErroGenerico(
            Throwable ex, HttpServletRequest request) {
        String traceId = obterTraceId();
        incrementarContador(ErrorCode.INTERNAL_ERROR);

        log.error("Erro interno não tratado. traceId={}", traceId, ex);

        ProblemDetails body = ProblemDetails.interno(traceId, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(PROBLEM_JSON).body(body);
    }

    // ── Utilitários ─────────────────────────────────────────

    private String obterTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }

    private void incrementarContador(ErrorCode codigo) {
        Counter.builder("errors_total")
                .tag("code", codigo.name())
                .register(meterRegistry)
                .increment();
    }

    private ErrorCode mapearCodigoPostgres(DataIntegrityViolationException ex) {
        String mensagem = ex.getMostSpecificCause().getMessage();
        if (mensagem != null) {
            if (mensagem.contains("23P01") || mensagem.contains("exclusion_violation")) {
                return ErrorCode.SLOT_ALREADY_TAKEN;
            }
        }
        return ErrorCode.VALIDATION_ERROR;
    }
}
