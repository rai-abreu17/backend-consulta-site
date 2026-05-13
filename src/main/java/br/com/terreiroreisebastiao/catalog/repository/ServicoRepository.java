package br.com.terreiroreisebastiao.catalog.repository;

import br.com.terreiroreisebastiao.catalog.domain.Servico;
import br.com.terreiroreisebastiao.catalog.domain.TipoServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de acesso a dados para a entidade {@link Servico}.
 *
 * <p>Inclui queries customizadas para listagem paginada com filtros
 * por tipo, categoria e status de publicação.</p>
 */
@Repository
public interface ServicoRepository extends JpaRepository<Servico, UUID> {

        /**
         * Busca um serviço com a categoria já carregada, útil para respostas administrativas.
         */
        @Query("""
                        SELECT s FROM Servico s
                        JOIN FETCH s.categoria c
                        WHERE s.id = :servicoId
                        """)
        Optional<Servico> buscarPorIdComCategoria(@Param("servicoId") UUID servicoId);

    /**
     * Busca um serviço publicado pelo slug.
     *
     * @param slug identificador amigável para URL
     * @return o serviço correspondente, se publicado
     */
    Optional<Servico> findBySlugAndPublicadoTrue(String slug);

    /**
     * Busca um serviço pelo slug (admin — independe de publicação).
     *
     * @param slug identificador amigável para URL
     * @return o serviço correspondente, se existir
     */
    Optional<Servico> findBySlug(String slug);

    /**
     * Verifica se já existe um serviço com o slug informado.
     *
     * @param slug identificador a verificar
     * @return {@code true} se o slug já está em uso
     */
    boolean existsBySlug(String slug);

        boolean existsBySlugAndIdNot(String slug, UUID id);

    /**
     * Verifica se existem serviços vinculados a uma categoria.
     *
     * @param categoriaId UUID da categoria
     * @return {@code true} se há serviços vinculados
     */
    boolean existsByCategoriaId(UUID categoriaId);

    /**
     * Lista serviços publicados com filtros opcionais por tipo e slug da categoria.
     *
     * @param tipo          tipo do serviço (ou {@code null} para todos)
     * @param categoriaSlug slug da categoria (ou {@code null} para todas)
     * @param pageable      configuração de paginação e ordenação
     * @return página de serviços publicados
     */
    @Query("""
            SELECT s FROM Servico s JOIN FETCH s.categoria c
            WHERE s.publicado = TRUE
              AND (:tipo IS NULL OR s.tipo = :tipo)
              AND (:categoriaSlug IS NULL OR c.slug = :categoriaSlug)
            """)
    Page<Servico> listarPublicados(
            @Param("tipo") TipoServico tipo,
            @Param("categoriaSlug") String categoriaSlug,
            Pageable pageable
    );

    /**
     * Lista todos os serviços (admin) com filtros opcionais.
     *
     * @param tipo          tipo do serviço (ou {@code null} para todos)
     * @param categoriaId   UUID da categoria (ou {@code null} para todas)
     * @param pageable      configuração de paginação e ordenação
     * @return página de serviços
     */
    @Query("""
            SELECT s FROM Servico s JOIN FETCH s.categoria c
            WHERE (:tipo IS NULL OR s.tipo = :tipo)
              AND (:categoriaId IS NULL OR c.id = :categoriaId)
            """)
    Page<Servico> listarTodos(
            @Param("tipo") TipoServico tipo,
            @Param("categoriaId") UUID categoriaId,
            Pageable pageable
    );
}
