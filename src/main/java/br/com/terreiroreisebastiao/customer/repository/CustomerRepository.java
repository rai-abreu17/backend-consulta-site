package br.com.terreiroreisebastiao.customer.repository;

import br.com.terreiroreisebastiao.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de acesso a dados para {@link Customer}.
 *
 * <p>A busca por email é feita exclusivamente pelo hash determinístico
 * ({@code email_lookup_hash}), evitando a necessidade de decifrar toda a
 * tabela para localizar um consulente.</p>
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Busca um consulente pelo hash determinístico do email.
     *
     * <p>O hash deve ser gerado via {@code PiiCipher.gerarHashParaBusca(email)}
     * antes de chamar este método.</p>
     *
     * @param emailLookupHash hash HMAC-SHA256 do email sanitizado
     * @return {@link Optional} contendo o consulente, ou vazio se não encontrado
     */
    Optional<Customer> findByEmailLookupHash(String emailLookupHash);

    /**
     * Verifica se já existe um consulente com o hash de email informado.
     *
     * @param emailLookupHash hash HMAC-SHA256 do email sanitizado
     * @return {@code true} se o hash já estiver cadastrado
     */
    boolean existsByEmailLookupHash(String emailLookupHash);
}
