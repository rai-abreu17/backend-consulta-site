package br.com.terreiroreisebastiao.catalog.dto;

import br.com.terreiroreisebastiao.catalog.domain.Categoria;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * DTO de resposta representando uma categoria do catálogo.
 *
 * @param id            UUID da categoria
 * @param slug          identificador amigável para URL
 * @param nome          nome de exibição
 * @param descricao     descrição opcional
 * @param ordemExibicao posição na listagem
 * @param publicada     se está visível ao público
 * @param criadoEm      data de criação
 */
public record RespostaCategoria(
        UUID id,
        String slug,
        @JsonProperty("name") String nome,
        @JsonProperty("description") String descricao,
        @JsonProperty("displayOrder") Short ordemExibicao,
        @JsonProperty("isPublished") Boolean publicada,
        @JsonProperty("createdAt") OffsetDateTime criadoEm
) {

    /**
     * Cria o DTO a partir da entidade {@link Categoria}.
     *
     * @param categoria entidade de domínio
     * @return DTO de resposta
     */
    public static RespostaCategoria de(Categoria categoria) {
        return new RespostaCategoria(
                categoria.getId(),
                categoria.getSlug(),
                categoria.getNome(),
                categoria.getDescricao(),
                categoria.getOrdemExibicao(),
                categoria.getPublicada(),
                categoria.getCriadoEm() != null
                        ? categoria.getCriadoEm().withOffsetSameInstant(ZoneOffset.UTC)
                        : null
        );
    }

    /**
     * Representação resumida de uma categoria, usada dentro de respostas de serviço.
     *
     * @param id   UUID da categoria
     * @param slug identificador amigável
     * @param nome nome de exibição
     */
    public record CategoriaResumo(
            UUID id,
            String slug,
            @JsonProperty("name") String nome
    ) {
    }
}
