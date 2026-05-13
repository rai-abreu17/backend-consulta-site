package br.com.terreiroreisebastiao.config;

import br.com.terreiroreisebastiao.auth.filter.CsrfCookieFilter;
import br.com.terreiroreisebastiao.auth.filter.JwtAuthenticationFilter;
import br.com.terreiroreisebastiao.auth.filter.RateLimitFilter;
import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import br.com.terreiroreisebastiao.shared.error.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.UUID;

/**
 * Configuração central do Spring Security.
 *
 * <p>Sessão stateless (JWT), CSRF nativo desabilitado (implementação própria via
 * {@link CsrfCookieFilter}), filtros na ordem: RateLimit → JWT → CSRF.</p>
 *
 * <p>AuthenticationEntryPoint e AccessDeniedHandler customizados retornam
 * {@link ProblemDetails} RFC 7807 diretamente.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtConfig.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CsrfCookieFilter csrfFilter;
    private final RateLimitFilter rateLimitFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          CsrfCookieFilter csrfFilter,
                          RateLimitFilter rateLimitFilter,
                          ObjectMapper objectMapper) {
        this.jwtFilter = jwtFilter;
        this.csrfFilter = csrfFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos (sem autenticação)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/public/webhooks/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhooks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/availability/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/content/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/public/bookings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/bookings/**").permitAll()
                        // Actuator e OpenAPI
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Rotas admin exigem autenticação
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, authException) -> {
                            String traceId = obterTraceId();
                        ErrorCode codigo = resolverCodigoJwt(request);
                            ProblemDetails problema = ProblemDetails.of(
                            codigo,
                            detalheAutenticacao(codigo),
                                    request.getRequestURI(), traceId
                            );
                        response.setStatus(codigo.getDefaultStatus().value());
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), problema);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String traceId = obterTraceId();
                            ProblemDetails problema = ProblemDetails.of(
                                    ErrorCode.FORBIDDEN,
                                    "Você não tem permissão para acessar este recurso.",
                                    request.getRequestURI(), traceId
                            );
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), problema);
                        })
                )
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(csrfFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt com cost 12, conforme SPEC. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private String obterTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }

    private ErrorCode resolverCodigoJwt(jakarta.servlet.http.HttpServletRequest request) {
        Object codigo = request.getAttribute(JwtAuthenticationFilter.REQUEST_ATTRIBUTE_JWT_FAILURE_CODE);
        if (codigo instanceof ErrorCode errorCode) {
            return errorCode;
        }
        return ErrorCode.UNAUTHENTICATED;
    }

    private String detalheAutenticacao(ErrorCode codigo) {
        return switch (codigo) {
            case TOKEN_EXPIRED -> "O token JWT expirou. Faça login novamente.";
            case TOKEN_INVALID -> "O token JWT informado é inválido.";
            default -> "Autenticação necessária para acessar este recurso.";
        };
    }
}
