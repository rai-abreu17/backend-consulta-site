package br.com.terreiroreisebastiao.shared.error;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representa um erro de validação por campo, retornado dentro do array {@code errors[]}
 * do {@link ProblemDetails}.
 *
 * @param field   nome do campo com erro (camelCase conforme JSON do contrato)
 * @param message mensagem legível para o usuário final
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldError(String field, String message) {
}
