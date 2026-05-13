package br.com.terreiroreisebastiao.auth.repository;

import br.com.terreiroreisebastiao.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de acesso a dados para {@link RefreshToken}.
 *
 * <p>Inclui queries para rotação segura de tokens e revogação em massa
 * (cenário de detecção de reuso, conforme Sprint 002 — F-002-02).</p>
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Busca um refresh token ativo (não revogado e não expirado) pelo hash.
     *
     * @param tokenHash hash SHA-256 do token
     * @param agora     instante de referência para verificar expiração
     * @return {@link Optional} contendo o token, ou vazio se inválido
     */
    @Query("""
            SELECT rt FROM RefreshToken rt
            WHERE rt.tokenHash = :tokenHash
              AND rt.revogadoEm IS NULL
              AND rt.expiraEm > :agora
            """)
    Optional<RefreshToken> buscarAtivoPorHash(
            @Param("tokenHash") String tokenHash,
            @Param("agora") OffsetDateTime agora
    );

    /**
     * Busca um refresh token pelo hash, independente do estado (revogado ou não).
     * Utilizado para detectar reuso de token já revogado.
     *
     * @param tokenHash hash SHA-256 do token
     * @return {@link Optional} contendo o token, ou vazio se não encontrado
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoga todos os refresh tokens ativos de um administrador específico.
     * Utilizado no cenário de detecção de reuso (suspeita de roubo).
     *
     * @param adminUserId ID do administrador
     * @param agora       instante de revogação
     * @return quantidade de tokens revogados
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revogadoEm = :agora
            WHERE rt.adminUser.id = :adminUserId
              AND rt.revogadoEm IS NULL
            """)
    int revogarTodosPorAdminUser(
            @Param("adminUserId") UUID adminUserId,
            @Param("agora") OffsetDateTime agora
    );
}
