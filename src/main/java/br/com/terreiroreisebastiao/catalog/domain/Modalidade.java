package br.com.terreiroreisebastiao.catalog.domain;

/**
 * Enum que define a modalidade de atendimento de um serviço.
 *
 * <p>Corresponde ao valor da tabela {@code service_modality_link}
 * conforme DDL V2 da SPEC §4.3.</p>
 */
public enum Modalidade {

    /** Atendimento realizado à distância (chamada de vídeo, telefone, etc.). */
    ONLINE,

    /** Atendimento presencial no terreiro. */
    IN_PERSON
}
