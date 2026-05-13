package br.com.terreiroreisebastiao.auth;

import br.com.terreiroreisebastiao.auth.controller.AuthController;
import br.com.terreiroreisebastiao.auth.domain.AdminUser;
import br.com.terreiroreisebastiao.auth.domain.RefreshToken;
import br.com.terreiroreisebastiao.auth.filter.CsrfCookieFilter;
import br.com.terreiroreisebastiao.auth.filter.JwtAuthenticationFilter;
import br.com.terreiroreisebastiao.auth.filter.RateLimitFilter;
import br.com.terreiroreisebastiao.auth.repository.AdminUserRepository;
import br.com.terreiroreisebastiao.auth.repository.RefreshTokenRepository;
import br.com.terreiroreisebastiao.auth.service.AuthService;
import br.com.terreiroreisebastiao.auth.service.CookieService;
import br.com.terreiroreisebastiao.auth.service.JwtService;
import br.com.terreiroreisebastiao.config.SecurityConfig;
import br.com.terreiroreisebastiao.shared.error.GlobalExceptionHandler;
import io.jsonwebtoken.Claims;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AuthLoginIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
            "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.redisson.spring.starter.RedissonAutoConfigurationV2",
                "jwt.secret=xTwKMKaTMGyZedymFWEuNnxGRlozXhxRXL4GE/T9xqo=",
                "jwt.expiration=24h",
                "jwt.refresh-ttl-days=30",
                "server.ssl.enabled=false"
        }
)
@AutoConfigureMockMvc(addFilters = true)
class AuthLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AdminUserRepository adminUserRepository;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    private AdminUser adminSeed;

    @BeforeEach
    void setUp() {
        adminSeed = new AdminUser(
                "admin@terreiro.app",
                passwordEncoder.encode("senha123"),
                "Administrador Inicial",
                "ADMIN"
        );
        ReflectionTestUtils.setField(adminSeed, "id", UUID.fromString("11111111-1111-1111-1111-111111111111"));

        when(adminUserRepository.findByEmail(eq("admin@terreiro.app"))).thenReturn(Optional.of(adminSeed));
        when(adminUserRepository.save(any(AdminUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveAutenticarAdminSeedERetornarJwtValidoNoCookie() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "email": "admin@terreiro.app",
                                  "password": "senha123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("admin@terreiro.app"))
                .andExpect(jsonPath("$.user.displayName").value("Administrador Inicial"))
                .andReturn();

        List<String> cookies = resultado.getResponse().getHeaders(HttpHeaders.SET_COOKIE);

        assertThat(cookies).anyMatch(cookie -> cookie.startsWith("access_token="));
        assertThat(cookies).anyMatch(cookie -> cookie.startsWith("refresh_token="));
        assertThat(cookies).anyMatch(cookie -> cookie.startsWith("csrf_token="));

        String cookieAccess = cookies.stream()
                .filter(cookie -> cookie.startsWith("access_token="))
                .findFirst()
                .orElseThrow();

        String token = extrairValorCookie(cookieAccess);
        assertThat(jwtService.tokenValido(token)).isTrue();

        Claims claims = jwtService.extrairClaims(token);
        assertThat(claims.getSubject()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(claims.get("email", String.class)).isEqualTo("admin@terreiro.app");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    private String extrairValorCookie(String setCookieHeader) {
        int indiceSeparador = setCookieHeader.indexOf(';');
        String cookieCompleto = indiceSeparador >= 0
                ? setCookieHeader.substring(0, indiceSeparador)
                : setCookieHeader;
        return cookieCompleto.substring(cookieCompleto.indexOf('=') + 1);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            SecurityConfig.class,
            AuthController.class,
            AuthService.class,
            JwtService.class,
            CookieService.class,
            JwtAuthenticationFilter.class,
            RateLimitFilter.class,
            CsrfCookieFilter.class,
            GlobalExceptionHandler.class
    })
    static class TestApplication {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}