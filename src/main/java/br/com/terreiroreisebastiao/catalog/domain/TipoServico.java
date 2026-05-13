package br.com.terreiroreisebastiao.catalog.domain;

/**
 * Enum que define o tipo de um serviço do catálogo.
 *
 * <p>Corresponde à coluna {@code type} da tabela {@code service}
 * conforme DDL V2 da SPEC §4.3.</p>
 */
public enum TipoServico {

    /** Consulta oracular (tarô, búzios, baralho cigano, etc.). */
    CONSULTATION,

    /** Ritual espiritual. */
    RITUAL
}
