package br.com.terreiroreisebastiao.shared.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Resposta de erro padronizada conforme RFC 7807 Problem Details.
 * Content-Type: {@code application/problem+json}.
 *
 * <p>Campos obrigatórios: type, title, status, detail, instance, code, traceId.
 * Campo opcional: errors[] — presente apenas em erros de validação.</p>
 *
 * @param type     URI identificadora do tipo de erro
 * @param title    título legível do erro
 * @param status   código HTTP
 * @param detail   mensagem detalhada para o cliente
 * @param instance path da requisição que gerou o erro
 * @param code     {@link ErrorCode#name()} — chave para o frontend mapear
 * @param traceId  W3C Trace Context — correlação ponta a ponta
 * @param errors   erros de validação por campo (pode ser vazio)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ProblemDetails(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String traceId,
        List<FieldError> errors
) {

    private static final String URI_BASE = "https://api.terreiro.app/errors/";

    /**
     * Cria um ProblemDetails a partir de um {@link ErrorCode}, usando o status padrão do código.
     */
    public static ProblemDetails of(ErrorCode errorCode, String detail, String instance, String traceId) {
        return new ProblemDetails(
                URI_BASE + errorCode.name(),
                errorCode.getTitulo(),
                errorCode.getDefaultStatus().value(),
                detail,
                instance,
                errorCode.name(),
                traceId,
                List.of()
        );
    }

    /**
     * Cria um ProblemDetails com status HTTP customizado (diferente do padrão do ErrorCode).
     */
    public static ProblemDetails of(ErrorCode errorCode, int status, String detail,
                                    String instance, String traceId) {
        return new ProblemDetails(
                URI_BASE + errorCode.name(),
                errorCode.getTitulo(),
                status,
                detail,
                instance,
                errorCode.name(),
                traceId,
                List.of()
        );
    }

    /**
     * Cria um ProblemDetails com erros de validação por campo.
     */
    public static ProblemDetails comErrosDeCampo(ErrorCode errorCode, String detail,
                                                 String instance, String traceId,
                                                 List<FieldError> errors) {
        return new ProblemDetails(
                URI_BASE + errorCode.name(),
                errorCode.getTitulo(),
                errorCode.getDefaultStatus().value(),
                detail,
                instance,
                errorCode.name(),
                traceId,
                errors
        );
    }

    /** Cria um ProblemDetails genérico para erro interno (500). */
    public static ProblemDetails interno(String traceId, String instance) {
        return of(
                ErrorCode.INTERNAL_ERROR,
                "Erro interno do servidor. Tente novamente mais tarde.",
                instance,
                traceId
        );
    }
}
