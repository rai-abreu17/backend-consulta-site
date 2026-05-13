package br.com.terreiroreisebastiao.catalog.domain;

import br.com.terreiroreisebastiao.shared.persistence.DatabaseGeneratedUuid;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidade que representa um serviço (consulta ou ritual) do catálogo.
 *
 * <p>Mapeia a tabela {@code service} criada pela migration {@code V2__catalog.sql}.
 * As modalidades são geridas via {@code service_modality_link} (relação N:N simplificada).</p>
 *
 * @see Categoria
 * @see Modalidade
 */
@Entity
@Table(name = "service")
@DynamicInsert
public class Servico {

    @Id
    @DatabaseGeneratedUuid
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TipoServico tipo;

    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "name", nullable = false, length = 160)
    private String nome;

    @Column(name = "short_description", length = 280)
    private String descricaoCurta;

    @Column(name = "long_description", columnDefinition = "TEXT")
    private String descricaoLonga;

    @Column(name = "duration_min", nullable = false)
    private Short duracaoMinutos;

    @Column(name = "price_cents", nullable = false)
    private Long precoCentavos;

    @ColumnDefault("'BRL'")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String moeda = "BRL";

    @ColumnDefault("false")
    @Column(name = "is_published", nullable = false)
    private Boolean publicado = false;

    @ColumnDefault("0")
    @Column(name = "display_order", nullable = false)
    private Short ordemExibicao = 0;

    @Generated(event = EventType.INSERT)
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime criadoEm;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime atualizadoEm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "service_modality_link",
            joinColumns = @JoinColumn(name = "service_id")
    )
    @Column(name = "modality")
    @Enumerated(EnumType.STRING)
    private Set<Modalidade> modalidades = new HashSet<>();

    /** Construtor padrão exigido pelo JPA. */
    protected Servico() {
    }

    /**
     * Construtor de conveniência para criação programática.
     *
     * @param categoria       categoria pai do serviço
     * @param tipo            tipo do serviço (CONSULTATION ou RITUAL)
     * @param slug            identificador amigável para URL
     * @param nome            nome de exibição
     * @param descricaoCurta  descrição resumida (máx. 280 chars)
     * @param descricaoLonga  descrição completa em Markdown
     * @param duracaoMinutos  duração em minutos
     * @param precoCentavos   preço em centavos (BRL)
     * @param modalidades     conjunto de modalidades de atendimento
     */
    public Servico(Categoria categoria, TipoServico tipo, String slug, String nome,
                   String descricaoCurta, String descricaoLonga,
                   Short duracaoMinutos, Long precoCentavos, Set<Modalidade> modalidades) {
        this.categoria = categoria;
        this.tipo = tipo;
        this.slug = slug;
        this.nome = nome;
        this.descricaoCurta = descricaoCurta;
        this.descricaoLonga = descricaoLonga;
        this.duracaoMinutos = duracaoMinutos;
        this.precoCentavos = precoCentavos;
        this.modalidades = modalidades != null ? modalidades : new HashSet<>();
    }

    // ──────────────────────────────────────────────
    //  Getters e Setters
    // ──────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public TipoServico getTipo() {
        return tipo;
    }

    public void setTipo(TipoServico tipo) {
        this.tipo = tipo;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricaoCurta() {
        return descricaoCurta;
    }

    public void setDescricaoCurta(String descricaoCurta) {
        this.descricaoCurta = descricaoCurta;
    }

    public String getDescricaoLonga() {
        return descricaoLonga;
    }

    public void setDescricaoLonga(String descricaoLonga) {
        this.descricaoLonga = descricaoLonga;
    }

    public Short getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(Short duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    public Long getPrecoCentavos() {
        return precoCentavos;
    }

    public void setPrecoCentavos(Long precoCentavos) {
        this.precoCentavos = precoCentavos;
    }

    public String getMoeda() {
        return moeda;
    }

    public Boolean getPublicado() {
        return publicado;
    }

    public void setPublicado(Boolean publicado) {
        this.publicado = publicado;
    }

    public Short getOrdemExibicao() {
        return ordemExibicao;
    }

    public void setOrdemExibicao(Short ordemExibicao) {
        this.ordemExibicao = ordemExibicao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public Set<Modalidade> getModalidades() {
        return modalidades;
    }

    public void setModalidades(Set<Modalidade> modalidades) {
        this.modalidades = modalidades != null ? modalidades : new HashSet<>();
    }
}
