package br.com.terreiroreisebastiao.auth.repository;

import br.com.terreiroreisebastiao.auth.domain.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de acesso a dados para {@link AdminUser}.
 *
 * <p>Não deve conter lógica de negócio — apenas assinaturas de queries
 * derivadas ou anotadas com {@code @Query}.</p>
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    /**
     * Busca um administrador pelo email (case-sensitive conforme banco).
     *
     * @param email email do administrador
     * @return {@link Optional} contendo o admin, ou vazio se não encontrado
     */
    Optional<AdminUser> findByEmail(String email);

    /**
     * Verifica se já existe um administrador com o email informado.
     *
     * @param email email a verificar
     * @return {@code true} se o email já estiver cadastrado
     */
    boolean existsByEmail(String email);
}
