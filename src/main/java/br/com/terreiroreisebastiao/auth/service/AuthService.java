package br.com.terreiroreisebastiao.auth.service;

import br.com.terreiroreisebastiao.auth.domain.AdminUser;
import br.com.terreiroreisebastiao.auth.domain.RefreshToken;
import br.com.terreiroreisebastiao.auth.dto.RespostaUsuario;
import br.com.terreiroreisebastiao.auth.repository.AdminUserRepository;
import br.com.terreiroreisebastiao.auth.repository.RefreshTokenRepository;
import br.com.terreiroreisebastiao.config.JwtConfig;
import br.com.terreiroreisebastiao.shared.error.ApiException;
import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Serviço de autenticação e gestão de sessão do administrador.
 *
 * <p>Implementa os fluxos de login, refresh, logout e consulta do usuário
 * autenticado conforme Sprint 002 da SPEC.</p>
 *
 * <p><strong>Regras de segurança:</strong></p>
 * <ul>
 *   <li>BCrypt dummy executado quando email não existe (mitigar timing attack)</li>
 *   <li>Bloqueio após 5 falhas consecutivas (brute-force protection)</li>
 *   <li>Rotação obrigatória de refresh token</li>
 *   <li>Detecção de reuso → revogação em massa</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final short LIMITE_TENTATIVAS = 5;
    private static final int MINUTOS_BLOQUEIO = 15;
    /** Hash fictício para BCrypt dummy — impede timing attacks de enumeração de email. */
    private static final String HASH_FICTICIO =
            "$2a$12$LJ3m4ys3X2y5y5y5y5y5yOuJd6fX6l.UGdV7WYs8E7jNqVzCcLfKC";

    private final AdminUserRepository adminUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final int tempoRefreshDias;

    public AuthService(AdminUserRepository adminUserRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CookieService cookieService,
                       JwtConfig jwtConfig) {
        this.adminUserRepository = adminUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.tempoRefreshDias = jwtConfig.refreshTtlDays();
    }

    // ── Login ───────────────────────────────────────────────

    /**
     * Autentica um administrador por email e senha.
     *
     * @param email     email informado
     * @param senha     senha em texto puro
     * @param userAgent user-agent do cliente
     * @param ip        endereço IP do cliente
     * @return resultado contendo cookies e dados do usuário
     */
    @Transactional
    public ResultadoAutenticacao autenticar(String email, String senha,
                                           String userAgent, String ip) {
        Optional<AdminUser> adminOpt = adminUserRepository.findByEmail(email);

        if (adminOpt.isEmpty()) {
            passwordEncoder.matches(senha, HASH_FICTICIO);
            throw criarExcecaoCredenciaisInvalidas();
        }

        AdminUser admin = adminOpt.get();

        verificarContaAtiva(admin);
        verificarBloqueio(admin);

        if (!passwordEncoder.matches(senha, admin.getSenhaHash())) {
            registrarFalhaDeLogin(admin);
            throw criarExcecaoCredenciaisInvalidas();
        }

        registrarSucessoDeLogin(admin);
        return gerarTokensECookies(admin, userAgent, ip);
    }

    // ── Refresh ─────────────────────────────────────────────

    /**
     * Renova os tokens de sessão a partir do refresh token.
     *
     * @param refreshTokenOpaque valor do refresh token lido do cookie
     * @param userAgent          user-agent do cliente
     * @param ip                 endereço IP do cliente
     * @return novo resultado com cookies atualizados
     */
    @Transactional
    public ResultadoAutenticacao renovarToken(String refreshTokenOpaque,
                                             String userAgent, String ip) {
        if (refreshTokenOpaque == null || refreshTokenOpaque.isBlank()) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Token de refresh ausente.");
        }

        String hash = calcularSha256(refreshTokenOpaque);
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        Optional<RefreshToken> ativoOpt = refreshTokenRepository.buscarAtivoPorHash(hash, agora);

        if (ativoOpt.isPresent()) {
            RefreshToken tokenAtivo = ativoOpt.get();
            tokenAtivo.setRevogadoEm(agora);
            refreshTokenRepository.save(tokenAtivo);

            AdminUser admin = tokenAtivo.getAdminUser();
            log.info("Refresh token rotacionado. adminId={}", admin.getId());
            return gerarTokensECookies(admin, userAgent, ip);
        }

        Optional<RefreshToken> qualquerOpt = refreshTokenRepository.findByTokenHash(hash);
        if (qualquerOpt.isPresent()) {
            RefreshToken tokenReusado = qualquerOpt.get();
            if (tokenReusado.estaRevogado()) {
                AdminUser admin = tokenReusado.getAdminUser();
                int revogados = refreshTokenRepository.revogarTodosPorAdminUser(admin.getId(), agora);
                log.warn("Reuso de refresh token detectado. adminId={} tokensRevogados={}",
                        admin.getId(), revogados);
                throw new ApiException(ErrorCode.TOKEN_INVALID,
                        "Sessão invalidada por motivos de segurança. Faça login novamente.");
            }
        }

        throw new ApiException(ErrorCode.TOKEN_INVALID, "Token de refresh inválido ou expirado.");
    }

    // ── Logout ──────────────────────────────────────────────

    /**
     * Encerra a sessão revogando o refresh token atual.
     *
     * @param refreshTokenOpaque valor do refresh token do cookie
     */
    @Transactional
    public void encerrarSessao(String refreshTokenOpaque) {
        if (refreshTokenOpaque == null || refreshTokenOpaque.isBlank()) {
            return;
        }

        String hash = calcularSha256(refreshTokenOpaque);
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        refreshTokenRepository.buscarAtivoPorHash(hash, agora)
                .ifPresent(token -> {
                    token.setRevogadoEm(agora);
                    refreshTokenRepository.save(token);
                    log.info("Logout realizado. adminId={}", token.getAdminUser().getId());
                });
    }

    // ── Me ───────────────────────────────────────────────────

    /**
     * Consulta os dados do administrador autenticado pelo ID.
     *
     * @param adminId UUID do administrador (extraído do JWT)
     * @return dados públicos do administrador
     */
    @Transactional(readOnly = true)
    public RespostaUsuario consultarUsuarioAutenticado(java.util.UUID adminId) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED,
                        "Administrador não encontrado."));
        return RespostaUsuario.de(admin);
    }

    // ── Métodos internos ────────────────────────────────────

    private void verificarContaAtiva(AdminUser admin) {
        if (!admin.getAtivo()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED, HttpStatus.FORBIDDEN,
                    "Conta desativada. Entre em contato com o administrador.");
        }
    }

    private void verificarBloqueio(AdminUser admin) {
        OffsetDateTime bloqueadoAte = admin.getBloqueadoAte();
        if (bloqueadoAte != null && bloqueadoAte.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED, HttpStatus.LOCKED,
                    "Conta bloqueada temporariamente. Tente novamente em alguns minutos.");
        }
    }

    private void registrarFalhaDeLogin(AdminUser admin) {
        short tentativas = (short) (admin.getTentativasFalhas() + 1);
        admin.setTentativasFalhas(tentativas);

        if (tentativas >= LIMITE_TENTATIVAS) {
            OffsetDateTime bloqueioAte = OffsetDateTime.now(ZoneOffset.UTC)
                    .plusMinutes(MINUTOS_BLOQUEIO);
            admin.setBloqueadoAte(bloqueioAte);
            admin.setTentativasFalhas((short) 0);
            log.warn("Conta bloqueada por brute-force. adminId={}", admin.getId());
        }

        adminUserRepository.save(admin);
    }

    private void registrarSucessoDeLogin(AdminUser admin) {
        admin.setTentativasFalhas((short) 0);
        admin.setBloqueadoAte(null);
        admin.setUltimoLoginEm(OffsetDateTime.now(ZoneOffset.UTC));
        adminUserRepository.save(admin);
    }

    private ResultadoAutenticacao gerarTokensECookies(AdminUser admin,
                                                      String userAgent, String ip) {
        String accessToken = jwtService.gerarAccessToken(admin);
        String refreshTokenOpaque = cookieService.gerarRefreshTokenOpaque();
        String csrfToken = cookieService.gerarCsrfToken();

        String refreshHash = calcularSha256(refreshTokenOpaque);
        OffsetDateTime expiracao = OffsetDateTime.now(ZoneOffset.UTC).plusDays(tempoRefreshDias);

        RefreshToken refreshTokenEntidade = new RefreshToken(
                admin, refreshHash, expiracao, userAgent, ip
        );
        refreshTokenRepository.save(refreshTokenEntidade);

        ResponseCookie cookieAccess = cookieService.criarCookieAccessToken(accessToken);
        ResponseCookie cookieRefresh = cookieService.criarCookieRefreshToken(refreshTokenOpaque);
        ResponseCookie cookieCsrf = cookieService.criarCookieCsrf(csrfToken);

        return new ResultadoAutenticacao(
            accessToken,
                RespostaUsuario.de(admin),
                cookieAccess, cookieRefresh, cookieCsrf
        );
    }

    private ApiException criarExcecaoCredenciaisInvalidas() {
        return new ApiException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                "E-mail ou senha incorretos.");
    }

    private String calcularSha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException("SHA-256 não disponível na JVM.", excecao);
        }
    }

    // ── Record de resultado ─────────────────────────────────

    /**
     * Resultado de uma autenticação bem-sucedida, contendo os cookies a serem
     * adicionados à resposta HTTP e os dados do usuário.
     */
    public record ResultadoAutenticacao(
            String accessToken,
            RespostaUsuario respostaUsuario,
            ResponseCookie cookieAccess,
            ResponseCookie cookieRefresh,
            ResponseCookie cookieCsrf
    ) {
    }
}
