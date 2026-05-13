package br.com.terreiroreisebastiao.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Enum canônico de todos os códigos de erro do sistema, conforme SPEC §5.2.
 *
 * <p>Cada código carrega o {@link HttpStatus} padrão associado e um título legível
 * em pt-BR. Nenhum controller ou service inventa código fora deste enum.</p>
 */
public enum ErrorCode {

    // ── Validação ──────────────────────────────────────────
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Erro de validação"),

    // ── Autenticação e Sessão ─────────────────────────────
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Não autenticado"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "Conta bloqueada"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Conta desativada"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token expirado"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Token inválido"),
    CSRF_TOKEN_MISMATCH(HttpStatus.FORBIDDEN, "Token CSRF inválido"),

    // ── Autorização ───────────────────────────────────────
    FORBIDDEN(HttpStatus.FORBIDDEN, "Acesso negado"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Recurso não encontrado"),

    // ── Agendamento ───────────────────────────────────────
    SLOT_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Horário indisponível"),
    SLOT_ALREADY_TAKEN(HttpStatus.CONFLICT, "Horário já reservado"),
    MODALITY_NOT_ALLOWED_ON_DAY(HttpStatus.UNPROCESSABLE_ENTITY, "Modalidade não permitida no dia"),
    BOOKING_NOT_HOLDABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Agendamento não pode ser reservado"),
    BOOKING_ALREADY_PAID(HttpStatus.UNPROCESSABLE_ENTITY, "Agendamento já pago"),

    // ── Pagamento ─────────────────────────────────────────
    PAYMENT_INIT_FAILED(HttpStatus.BAD_GATEWAY, "Falha ao iniciar pagamento"),
    PAYMENT_WEBHOOK_INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "Assinatura de webhook inválida"),
    PAYMENT_WEBHOOK_DUPLICATE(HttpStatus.OK, "Webhook duplicado"),

    // ── Catálogo ──────────────────────────────────────────
    CATEGORY_HAS_SERVICES(HttpStatus.CONFLICT, "Categoria possui serviços vinculados"),
    SERVICE_HAS_FUTURE_BOOKINGS(HttpStatus.CONFLICT, "Serviço possui agendamentos futuros"),

    // ── Infraestrutura ────────────────────────────────────
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Limite de requisições excedido"),
    EXTERNAL_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "Serviço externo não respondeu"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor");

    private final HttpStatus defaultStatus;
    private final String titulo;

    ErrorCode(HttpStatus defaultStatus, String titulo) {
        this.defaultStatus = defaultStatus;
        this.titulo = titulo;
    }

    public HttpStatus getDefaultStatus() {
        return defaultStatus;
    }

    public String getTitulo() {
        return titulo;
    }
}
