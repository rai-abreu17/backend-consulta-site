package br.com.terreiroreisebastiao.shared.error;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Exceção base de domínio do sistema do Terreiro de Rei Sebastião.
 *
 * <p>Toda exceção de negócio herda desta classe. O {@code GlobalExceptionHandler}
 * captura com um único {@code @ExceptionHandler(ApiException.class)} e converte
 * para {@link ProblemDetails} RFC 7807.</p>
 *
 * <p>O {@link ErrorCode} define o código canônico e o {@link HttpStatus} padrão.
 * O status pode ser sobrescrito quando necessário (ex: {@code ACCOUNT_LOCKED} → 423).</p>
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;
    private final List<FieldError> fieldErrors;

    public ApiException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
        this.status = code.getDefaultStatus();
        this.fieldErrors = List.of();
    }

    public ApiException(ErrorCode code, HttpStatus status, String detail) {
        super(detail);
        this.code = code;
        this.status = status;
        this.fieldErrors = List.of();
    }

    public ApiException(ErrorCode code, String detail, List<FieldError> fieldErrors) {
        super(detail);
        this.code = code;
        this.status = code.getDefaultStatus();
        this.fieldErrors = fieldErrors != null ? fieldErrors : List.of();
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
