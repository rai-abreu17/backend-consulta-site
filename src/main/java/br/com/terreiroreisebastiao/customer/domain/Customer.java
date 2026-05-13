package br.com.terreiroreisebastiao.customer.domain;

import br.com.terreiroreisebastiao.shared.crypto.PiiAttributeConverter;
import br.com.terreiroreisebastiao.shared.persistence.DatabaseGeneratedUuid;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidade que representa um Consulente do Terreiro de Rei Sebastião.
 *
 * <p>Os campos de PII ({@code nomeCompleto}, {@code email}, {@code telefone})
 * são criptografados automaticamente pelo {@link PiiAttributeConverter}
 * (AES-GCM-256 via Google Tink) ao persistir no banco, e descriptografados
 * ao carregar em memória.</p>
 *
 * <p>O campo {@code emailLookupHash} armazena o HMAC-SHA256 do email sanitizado,
 * permitindo buscas determinísticas sem decifrar a tabela inteira.</p>
 *
 * @see PiiAttributeConverter
 * @see br.com.terreiroreisebastiao.customer.repository.CustomerRepository
 */
@Entity
@Table(name = "customer")
@DynamicInsert
public class Customer {

    @Id
    @DatabaseGeneratedUuid
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Convert(converter = PiiAttributeConverter.class)
    @Column(name = "full_name_enc", nullable = false, columnDefinition = "TEXT")
    private String nomeCompleto;

    @Convert(converter = PiiAttributeConverter.class)
    @Column(name = "email_enc", nullable = false, columnDefinition = "TEXT")
    private String email;

    @Convert(converter = PiiAttributeConverter.class)
    @Column(name = "phone_enc", nullable = false, columnDefinition = "TEXT")
    private String telefone;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "email_lookup_hash", nullable = false, unique = true, length = 64, columnDefinition = "CHAR(64)")
    private String emailLookupHash;

    @ColumnDefault("false")
    @Column(name = "is_anonymized", nullable = false)
    private Boolean anonimizado = false;

    @Column(name = "anonymized_at")
    private OffsetDateTime anonimizadoEm;

    @Generated(event = EventType.INSERT)
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime criadoEm;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime atualizadoEm;

    /** Construtor padrão exigido pelo JPA. */
    protected Customer() {
    }

    /**
     * Construtor de conveniência para criação programática.
     *
     * <p><strong>Atenção:</strong> os campos {@code nomeCompleto}, {@code email} e
     * {@code telefone} devem ser passados em texto puro — a criptografia é automática
     * pelo JPA {@link PiiAttributeConverter} no momento da persistência.</p>
     *
     * @param nomeCompleto    nome completo do consulente (texto puro)
     * @param email           email do consulente (texto puro)
     * @param telefone        telefone no formato E.164 (texto puro)
     * @param emailLookupHash hash HMAC-SHA256 do email para busca determinística
     */
    public Customer(String nomeCompleto, String email, String telefone, String emailLookupHash) {
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.telefone = telefone;
        this.emailLookupHash = emailLookupHash;
    }

    // ──────────────────────────────────────────────
    //  Getters e Setters
    // ──────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public String getEmailDec() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmailLookupHash() {
        return emailLookupHash;
    }

    public void setEmailLookupHash(String emailLookupHash) {
        this.emailLookupHash = emailLookupHash;
    }

    public Boolean getAnonimizado() {
        return anonimizado;
    }

    public void setAnonimizado(Boolean anonimizado) {
        this.anonimizado = anonimizado;
    }

    public OffsetDateTime getAnonimizadoEm() {
        return anonimizadoEm;
    }

    public void setAnonimizadoEm(OffsetDateTime anonimizadoEm) {
        this.anonimizadoEm = anonimizadoEm;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
