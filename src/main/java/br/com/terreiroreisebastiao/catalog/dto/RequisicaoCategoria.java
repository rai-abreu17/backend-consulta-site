package br.com.terreiroreisebastiao.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para criação de uma categoria.
 *
 * <p>Usado nos endpoints {@code POST /api/v1/admin/categorias}
 * e {@code PUT /api/v1/admin/categorias/{id}}.</p>
 *
 * @param nome          nome de exibição da categoria (3 a 120 caracteres)
 * @param descricao     descrição opcional da categoria
 * @param ordemExibicao posição na listagem (padrão 0)
 */
public record RequisicaoCategoria(

        @NotBlank(message = "O nome da categoria é obrigatório.")
        @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.")
        @JsonProperty("name")
        String nome,

        @JsonProperty("description")
        String descricao,

        @Min(value = 0, message = "A ordem de exibição não pode ser negativa.")
        @JsonProperty("displayOrder")
        Short ordemExibicao
) {
}
