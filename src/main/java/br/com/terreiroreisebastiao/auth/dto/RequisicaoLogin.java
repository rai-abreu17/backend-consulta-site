package br.com.terreiroreisebastiao.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para o endpoint {@code POST /api/v1/auth/login}.
 *
 * @param email endereço de email do administrador
 * @param senha senha em texto puro (nunca logada)
 */
public record RequisicaoLogin(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail em formato inválido.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @JsonProperty("password")
        String senha
) {
}
