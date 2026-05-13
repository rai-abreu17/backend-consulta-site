package br.com.terreiroreisebastiao.auth.filter;

import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import br.com.terreiroreisebastiao.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filtro de autenticação JWT que lê o token do cookie {@code access_token}.
 *
 * <p>Para cada requisição, tenta extrair e validar o JWT do cookie.
 * Se válido, popula o {@link SecurityContextHolder} com o
 * {@link UsernamePasswordAuthenticationToken} contendo o UUID do admin
 * e suas authorities ({@code ROLE_ADMIN} ou {@code ROLE_SUPER_ADMIN}).</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTRIBUTE_JWT_FAILURE_CODE = "jwt.failure.code";
    public static final String REQUEST_ATTRIBUTE_JWT_FAILURE_CAUSE = "jwt.failure.cause";

    private static final String COOKIE_ACCESS = "access_token";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extrairTokenDoHeader(request);
        if (token == null) {
            token = extrairTokenDoCookie(request);
        }

        if (token != null) {
            try {
                Claims claims = jwtService.extrairClaims(token);
                UUID adminId = UUID.fromString(claims.getSubject());
                String papel = claims.get("role", String.class);

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + papel)
                );

                UsernamePasswordAuthenticationToken autenticacao =
                        new UsernamePasswordAuthenticationToken(adminId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(autenticacao);

            } catch (ExpiredJwtException ex) {
                SecurityContextHolder.clearContext();
                request.setAttribute(REQUEST_ATTRIBUTE_JWT_FAILURE_CODE, ErrorCode.TOKEN_EXPIRED);
                request.setAttribute(REQUEST_ATTRIBUTE_JWT_FAILURE_CAUSE, ex);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                request.setAttribute(REQUEST_ATTRIBUTE_JWT_FAILURE_CODE, ErrorCode.TOKEN_INVALID);
                request.setAttribute(REQUEST_ATTRIBUTE_JWT_FAILURE_CAUSE, ex);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extrairTokenDoCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (COOKIE_ACCESS.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String extrairTokenDoHeader(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header != null && header.startsWith(PREFIXO_BEARER)) {
            return header.substring(PREFIXO_BEARER.length());
        }
        return null;
    }
}
