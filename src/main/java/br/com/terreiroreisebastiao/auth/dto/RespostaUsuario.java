package br.com.terreiroreisebastiao.auth.dto;

import br.com.terreiroreisebastiao.auth.domain.AdminUser;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * DTO de resposta contendo dados do administrador autenticado.
 * Usado em {@code POST /auth/login} e {@code GET /auth/me}.
 *
 * @param usuario dados públicos do administrador
 */
public record RespostaUsuario(@JsonProperty("user") DadosUsuario usuario) {

    /**
     * Dados públicos do administrador (sem informações sensíveis).
     *
     * @param id           UUID do administrador
     * @param email        email de acesso
     * @param nomeExibicao nome de exibição no painel
     * @param papel        papel de acesso (ADMIN ou SUPER_ADMIN)
     */
    public record DadosUsuario(
            UUID id,
            String email,
            @JsonProperty("displayName") String nomeExibicao,
            @JsonProperty("role") String papel
    ) {
    }

    /** Cria a resposta a partir de uma entidade {@link AdminUser}. */
    public static RespostaUsuario de(AdminUser admin) {
        return new RespostaUsuario(
                new DadosUsuario(
                        admin.getId(),
                        admin.getEmail(),
                        admin.getNomeExibicao(),
                        admin.getPapel()
                )
        );
    }
}
