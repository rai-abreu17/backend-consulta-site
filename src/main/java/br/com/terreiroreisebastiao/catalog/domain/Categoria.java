package br.com.terreiroreisebastiao.catalog.domain;

import br.com.terreiroreisebastiao.shared.persistence.DatabaseGeneratedUuid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa uma categoria do catálogo de serviços.
 *
 * <p>Mapeia a tabela {@code category} criada pela migration {@code V2__catalog.sql}.
 * Categorias agrupam serviços por tipo (ex.: "Consultas", "Rituais").</p>
 *
 * @see Servico
 */
@Entity
@Table(name = "category")
@DynamicInsert
public class Categoria {

    @Id
    @DatabaseGeneratedUuid
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "name", nullable = false, length = 120)
    private String nome;

    @Column(name = "description", columnDefinition = "TEXT")
    private String descricao;

    @ColumnDefault("0")
    @Column(name = "display_order", nullable = false)
    private Short ordemExibicao = 0;

    @ColumnDefault("true")
    @Column(name = "is_published", nullable = false)
    private Boolean publicada = true;

    @Generated(event = EventType.INSERT)
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime criadoEm;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime atualizadoEm;

    @OneToMany(mappedBy = "categoria")
    private List<Servico> servicos = new ArrayList<>();

    /** Construtor padrão exigido pelo JPA. */
    protected Categoria() {
    }

    /**
     * Construtor de conveniência para criação programática.
     *
     * @param slug          identificador amigável para URL
     * @param nome          nome de exibição da categoria
     * @param descricao     descrição opcional
     * @param ordemExibicao posição na listagem
     */
    public Categoria(String slug, String nome, String descricao, Short ordemExibicao) {
        this.slug = slug;
        this.nome = nome;
        this.descricao = descricao;
        this.ordemExibicao = ordemExibicao;
    }

    // ──────────────────────────────────────────────
    //  Getters e Setters
    // ──────────────────────────────────────────────

    public UUID getId() {
        return id;
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Short getOrdemExibicao() {
        return ordemExibicao;
    }

    public void setOrdemExibicao(Short ordemExibicao) {
        this.ordemExibicao = ordemExibicao;
    }

    public Boolean getPublicada() {
        return publicada;
    }

    public void setPublicada(Boolean publicada) {
        this.publicada = publicada;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public List<Servico> getServicos() {
        return servicos;
    }
}
