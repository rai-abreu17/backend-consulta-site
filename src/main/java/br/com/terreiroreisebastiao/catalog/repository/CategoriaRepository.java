package br.com.terreiroreisebastiao.catalog.repository;

import br.com.terreiroreisebastiao.catalog.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de acesso a dados para a entidade {@link Categoria}.
 *
 * <p>Métodos de consulta derivados via Spring Data JPA,
 * ordenação padrão por {@code ordemExibicao} ascendente.</p>
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    /**
     * Busca uma categoria pelo slug (único).
     *
     * @param slug identificador amigável para URL
     * @return a categoria correspondente, se existir
     */
    Optional<Categoria> findBySlug(String slug);

    /**
     * Verifica se já existe uma categoria com o slug informado.
     *
     * @param slug identificador a verificar
     * @return {@code true} se o slug já está em uso
     */
    boolean existsBySlug(String slug);

    /**
     * Lista todas as categorias publicadas, ordenadas pela posição de exibição.
     *
     * @return lista ordenada de categorias publicadas
     */
    List<Categoria> findByPublicadaTrueOrderByOrdemExibicaoAsc();

    /**
     * Lista todas as categorias (admin), ordenadas pela posição de exibição.
     *
     * @return lista ordenada de todas as categorias
     */
    List<Categoria> findAllByOrderByOrdemExibicaoAsc();
}
