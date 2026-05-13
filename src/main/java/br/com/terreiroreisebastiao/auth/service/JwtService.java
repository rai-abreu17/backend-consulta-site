package br.com.terreiroreisebastiao.auth.service;

import br.com.terreiroreisebastiao.auth.domain.AdminUser;
import br.com.terreiroreisebastiao.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * Serviço responsável pela geração e validação de JWT (access tokens).
 *
 * <p>Utiliza JJWT (io.jsonwebtoken) com HMAC-SHA256. O payload contém:
 * {@code sub} (UUID), {@code email}, {@code role}.</p>
 */
@Component
public class JwtService {

    private final SecretKey chaveSecreta;
    private final Duration expiracao;

    public JwtService(JwtConfig config) {
        this.chaveSecreta = Keys.hmacShaKeyFor(Decoders.BASE64.decode(config.secret()));
        this.expiracao = config.expiration();
    }

    /**
     * Gera um access token JWT para o administrador informado.
     *
     * @param admin entidade do administrador autenticado
     * @return token JWT assinado
     */
    public String gerarAccessToken(AdminUser admin) {
        Instant agora = Instant.now();

        return Jwts.builder()
                .subject(admin.getId().toString())
                .claim("email", admin.getEmail())
                .claim("role", admin.getPapel())
                .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plus(expiracao)))
                .signWith(chaveSecreta)
                .compact();
    }

    /**
     * Extrai as claims de um token JWT válido.
     *
     * @param token JWT a ser validado e parseado
     * @return claims do payload
     * @throws JwtException se o token for inválido ou expirado
     */
    public Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chaveSecreta)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verifica se o token é válido (assinatura correta e não expirado).
     *
     * @param token JWT a ser validado
     * @return {@code true} se válido
     */
    public boolean tokenValido(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Extrai o UUID do sujeito (sub) de um token.
     *
     * @param token JWT válido
     * @return UUID do administrador
     */
    public UUID extrairIdDoSujeito(String token) {
        return UUID.fromString(extrairClaims(token).getSubject());
    }

    /**
     * Verifica se o token está expirado.
     *
     * @param token JWT a ser verificado
     * @return {@code true} se expirado
     */
    public boolean tokenExpirado(String token) {
        try {
            extrairClaims(token);
            return false;
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (JwtException ex) {
            return true;
        }
    }
}
