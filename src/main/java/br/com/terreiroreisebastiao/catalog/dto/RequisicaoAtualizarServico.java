package br.com.terreiroreisebastiao.catalog.dto;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * DTO de entrada para atualização parcial de um serviço (PATCH).
 *
 * <p>Todos os campos são opcionais — apenas os informados serão atualizados.
 * Usado em fluxos internos de atualização parcial do catálogo.</p>
 *
 * @param categoriaId    nova categoria pai (ou {@code null} para manter)
 * @param slug           novo slug amigável (ou {@code null} para manter)
 * @param nome           novo nome (ou {@code null} para manter)
 * @param descricaoCurta nova descrição curta (ou {@code null} para manter)
 * @param descricaoLonga nova descrição longa (ou {@code null} para manter)
 * @param duracaoMinutos nova duração (ou {@code null} para manter)
 * @param precoCentavos  novo preço em centavos (ou {@code null} para manter)
 * @param modalidades    novo conjunto de modalidades (ou {@code null} para manter)
 * @param ordemExibicao  nova ordem de exibição (ou {@code null} para manter)
 */
public record RequisicaoAtualizarServico(

        @JsonProperty("categoryId")
        UUID categoriaId,

        @Size(min = 3, max = 120, message = "O slug deve ter entre 3 e 120 caracteres quando informado.")
        @JsonProperty("slug")
        String slug,

        @Size(min = 3, max = 160, message = "O nome deve ter entre 3 e 160 caracteres.")
        @JsonProperty("name")
        String nome,

        @Size(max = 280, message = "A descrição curta deve ter no máximo 280 caracteres.")
        @JsonProperty("shortDescription")
        String descricaoCurta,

        @JsonProperty("longDescription")
        String descricaoLonga,

        @Min(value = 15, message = "A duração mínima é de 15 minutos.")
        @Max(value = 480, message = "A duração máxima é de 480 minutos.")
        @JsonProperty("durationMin")
        Short duracaoMinutos,

        @Min(value = 0, message = "O preço não pode ser negativo.")
        @JsonProperty("priceCents")
        Long precoCentavos,

        @JsonProperty("modalities")
        Set<Modalidade> modalities,

        @JsonProperty("displayOrder")
        Short ordemExibicao
) {
}
