package br.com.terreiroreisebastiao.catalog.dto;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * DTO de entrada para criação de um serviço do catálogo.
 *
 * <p>Usado nos endpoints {@code POST /api/v1/admin/servicos}
 * e {@code PUT /api/v1/admin/servicos/{id}}.
 * As validações de negócio adicionais são aplicadas no {@code CatalogoService}.</p>
 *
 * @param categoriaId    UUID da categoria pai
 * @param tipo           tipo do serviço ("CONSULTATION" ou "RITUAL")
 * @param slug           slug amigável para URL (opcional; se ausente, será gerado do nome)
 * @param nome           nome de exibição (3 a 160 caracteres)
 * @param descricaoCurta descrição resumida (máx. 280 caracteres)
 * @param descricaoLonga descrição completa em Markdown
 * @param duracaoMinutos duração em minutos (15 a 480, múltiplo de 15)
 * @param precoCentavos  preço em centavos (≥ 0)
 * @param modalidades    conjunto de modalidades de atendimento (não vazio)
 */
public record RequisicaoCriarServico(

        @NotNull(message = "A categoria é obrigatória.")
        @JsonProperty("categoryId")
        UUID categoriaId,

        @NotBlank(message = "O tipo do serviço é obrigatório.")
        @JsonProperty("type")
        String type,

        @Size(min = 3, max = 120, message = "O slug deve ter entre 3 e 120 caracteres quando informado.")
        @JsonProperty("slug")
        String slug,

        @NotBlank(message = "O nome do serviço é obrigatório.")
        @Size(min = 3, max = 160, message = "O nome deve ter entre 3 e 160 caracteres.")
        @JsonProperty("name")
        String nome,

        @Size(max = 280, message = "A descrição curta deve ter no máximo 280 caracteres.")
        @JsonProperty("shortDescription")
        String descricaoCurta,

        @JsonProperty("longDescription")
        String descricaoLonga,

        @NotNull(message = "A duração é obrigatória.")
        @Min(value = 15, message = "A duração mínima é de 15 minutos.")
        @Max(value = 480, message = "A duração máxima é de 480 minutos.")
        @JsonProperty("durationMin")
        Short duracaoMinutos,

        @NotNull(message = "O preço é obrigatório.")
        @Min(value = 0, message = "O preço não pode ser negativo.")
        @JsonProperty("priceCents")
        Long precoCentavos,

        @NotEmpty(message = "Ao menos uma modalidade é obrigatória.")
        @JsonProperty("modalities")
        Set<Modalidade> modalities
) {
}
