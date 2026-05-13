package br.com.terreiroreisebastiao.auth.domain;

import br.com.terreiroreisebastiao.shared.persistence.DatabaseGeneratedUuid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidade que representa o administrador do Terreiro de Rei Sebastião.
 *
 * <p>Mapeia a tabela {@code admin_user} criada pela migration {@code V1__init_schema.sql}.
 * A autenticação utiliza BCrypt (cost 12) para o hash da senha.
 * O controle de brute-force é feito via {@code tentativasFalhas} e {@code bloqueadoAte}.</p>
 *
 * @see br.com.terreiroreisebastiao.auth.repository.AdminUserRepository
 */
@Entity
@Table(name = "admin_user")
@DynamicInsert
public class AdminUser {

    @Id
    @DatabaseGeneratedUuid
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String senhaHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String nomeExibicao;

    @ColumnDefault("'ADMIN'")
    @Column(name = "role", nullable = false, length = 20)
    private String papel = "ADMIN";

    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean ativo = true;

    @ColumnDefault("0")
    @Column(name = "failed_attempts", nullable = false)
    private Short tentativasFalhas = 0;

    @Column(name = "locked_until")
    private OffsetDateTime bloqueadoAte;

    @Column(name = "last_login_at")
    private OffsetDateTime ultimoLoginEm;

    @Generated(event = EventType.INSERT)
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime criadoEm;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime atualizadoEm;

    /** Construtor padrão exigido pelo JPA. */
    protected AdminUser() {
    }

    /**
     * Construtor de conveniência para criação programática.
     *
     * @param email        email do administrador (único no sistema)
     * @param senhaHash    hash BCrypt (cost 12) da senha
     * @param nomeExibicao nome de exibição no painel
     * @param papel        papel do administrador ({@code ADMIN} ou {@code SUPER_ADMIN})
     */
    public AdminUser(String email, String senhaHash, String nomeExibicao, String papel) {
        this.email = email;
        this.senhaHash = senhaHash;
        this.nomeExibicao = nomeExibicao;
        this.papel = papel;
    }

    // ──────────────────────────────────────────────
    //  Getters e Setters
    // ──────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public void setNomeExibicao(String nomeExibicao) {
        this.nomeExibicao = nomeExibicao;
    }

    public String getPapel() {
        return papel;
    }

    public void setPapel(String papel) {
        this.papel = papel;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Short getTentativasFalhas() {
        return tentativasFalhas;
    }

    public void setTentativasFalhas(Short tentativasFalhas) {
        this.tentativasFalhas = tentativasFalhas;
    }

    public OffsetDateTime getBloqueadoAte() {
        return bloqueadoAte;
    }

    public void setBloqueadoAte(OffsetDateTime bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
    }

    public OffsetDateTime getUltimoLoginEm() {
        return ultimoLoginEm;
    }

    public void setUltimoLoginEm(OffsetDateTime ultimoLoginEm) {
        this.ultimoLoginEm = ultimoLoginEm;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
