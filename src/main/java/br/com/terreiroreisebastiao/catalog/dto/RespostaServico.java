package br.com.terreiroreisebastiao.catalog.dto;

import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import br.com.terreiroreisebastiao.catalog.domain.Servico;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

/**
 * DTO de resposta representando um serviço do catálogo.
 *
 * @param id             UUID do serviço
 * @param slug           identificador amigável para URL
 * @param tipo           tipo do serviço (CONSULTATION ou RITUAL)
 * @param nome           nome de exibição
 * @param categoria      dados resumidos da categoria pai
 * @param descricaoCurta descrição resumida
 * @param descricaoLonga descrição completa em Markdown
 * @param duracaoMinutos duração em minutos
 * @param precoCentavos  preço em centavos
 * @param moeda          código da moeda (BRL)
 * @param modalidades    modalidades de atendimento
 * @param publicado      se está visível ao público
 * @param ordemExibicao  posição na listagem
 * @param criadoEm       data de criação
 */
public record RespostaServico(
        UUID id,
        String slug,
        String type,
        @JsonProperty("name") String nome,
        @JsonProperty("category") RespostaCategoria.CategoriaResumo categoria,
        @JsonProperty("shortDescription") String descricaoCurta,
        @JsonProperty("longDescription") String descricaoLonga,
        @JsonProperty("durationMin") Short duracaoMinutos,
        @JsonProperty("priceCents") Long precoCentavos,
        String currency,
        Set<Modalidade> modalities,
        @JsonProperty("isPublished") Boolean publicado,
        @JsonProperty("displayOrder") Short ordemExibicao,
        @JsonProperty("createdAt") OffsetDateTime criadoEm
) {

    /**
     * Cria o DTO a partir da entidade {@link Servico}.
     *
     * @param servico entidade de domínio (com categoria carregada)
     * @return DTO de resposta
     */
    public static RespostaServico de(Servico servico) {
        return new RespostaServico(
                servico.getId(),
                servico.getSlug(),
                servico.getTipo().name(),
                servico.getNome(),
                new RespostaCategoria.CategoriaResumo(
                        servico.getCategoria().getId(),
                        servico.getCategoria().getSlug(),
                        servico.getCategoria().getNome()
                ),
                servico.getDescricaoCurta(),
                servico.getDescricaoLonga(),
                servico.getDuracaoMinutos(),
                servico.getPrecoCentavos(),
                servico.getMoeda(),
                servico.getModalidades(),
                servico.getPublicado(),
                servico.getOrdemExibicao(),
                servico.getCriadoEm() != null
                        ? servico.getCriadoEm().withOffsetSameInstant(ZoneOffset.UTC)
                        : null
        );
    }
}
