package br.com.terreiroreisebastiao.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Entidade que representa um token de atualização (refresh token) do administrador.
 *
 * <p>O token em si nunca é persistido — apenas o hash SHA-256 de 64 caracteres
 * hexadecimais. A rotação é obrigatória: ao usar um refresh token, ele é revogado
 * e um novo é emitido.</p>
 *
 * @see AdminUser
 * @see br.com.terreiroreisebastiao.auth.repository.RefreshTokenRepository
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private AdminUser adminUser;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, unique = true, length = 64, columnDefinition = "CHAR(64)")
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiraEm;

    @Column(name = "revoked_at")
    private OffsetDateTime revogadoEm;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip", columnDefinition = "inet")
    private String ip;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    /** Construtor padrão exigido pelo JPA. */
    protected RefreshToken() {
    }

    /**
     * Construtor de conveniência para criação programática.
     *
     * @param adminUser administrador dono do token
     * @param tokenHash hash SHA-256 do token de refresh
     * @param expiraEm  momento de expiração do token
     * @param userAgent user-agent do cliente que solicitou o token
     * @param ip        endereço IP do cliente
     */
    public RefreshToken(AdminUser adminUser, String tokenHash, OffsetDateTime expiraEm,
                        String userAgent, String ip) {
        this.adminUser = adminUser;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
        this.userAgent = userAgent;
        this.ip = ip;
    }

    // ──────────────────────────────────────────────
    //  Getters e Setters
    // ──────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public AdminUser getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(AdminUser adminUser) {
        this.adminUser = adminUser;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public OffsetDateTime getExpiraEm() {
        return expiraEm;
    }

    public void setExpiraEm(OffsetDateTime expiraEm) {
        this.expiraEm = expiraEm;
    }

    public OffsetDateTime getRevogadoEm() {
        return revogadoEm;
    }

    public void setRevogadoEm(OffsetDateTime revogadoEm) {
        this.revogadoEm = revogadoEm;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    @PrePersist
    void prepararPersistencia() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        if (criadoEm == null) {
            criadoEm = agora;
        }
        atualizadoEm = agora;
    }

    @PreUpdate
    void prepararAtualizacao() {
        atualizadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Verifica se este token já foi revogado.
     *
     * @return {@code true} se o token foi revogado
     */
    public boolean estaRevogado() {
        return revogadoEm != null;
    }

    /**
     * Verifica se este token já expirou em relação ao instante informado.
     *
     * @param agora instante de referência
     * @return {@code true} se o token expirou
     */
    public boolean estaExpirado(OffsetDateTime agora) {
        return expiraEm.isBefore(agora);
    }
}
