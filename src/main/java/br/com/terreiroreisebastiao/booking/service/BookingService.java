package br.com.terreiroreisebastiao.booking.service;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.booking.domain.BookingStatus;
import br.com.terreiroreisebastiao.booking.dto.BookingAdminDto;
import br.com.terreiroreisebastiao.booking.exception.SlotUnavailableException;
import br.com.terreiroreisebastiao.booking.repository.BookingRepository;
import br.com.terreiroreisebastiao.booking.repository.MarcacaoRepository;
import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import br.com.terreiroreisebastiao.catalog.domain.Servico;
import br.com.terreiroreisebastiao.catalog.repository.ServicoRepository;
import br.com.terreiroreisebastiao.customer.domain.Customer;
import br.com.terreiroreisebastiao.customer.repository.CustomerRepository;
import br.com.terreiroreisebastiao.payment.service.MercadoPagoPreferenceService;
import br.com.terreiroreisebastiao.shared.crypto.PiiCipher;
import br.com.terreiroreisebastiao.shared.error.ApiException;
import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final long HOLD_RESERVA_MINUTOS = 10L;
    private static final long LOCK_WAIT_TIME_SECONDS = 1L;
    private static final long LOCK_LEASE_TIME_SECONDS = 30L;

    /**
     * Wrapper retornado por {@link #criarReservaPendente} contendo o booking criado
     * e a URL de checkout gerada pelo Mercado Pago.
     */
    public record ResultadoReservaPendente(Booking booking, String checkoutUrl) {}

    private final BookingRepository bookingRepository;
    private final MarcacaoRepository marcacaoRepository;
    private final ServicoRepository servicoRepository;
    private final CustomerRepository customerRepository;
    private final RedissonClient redissonClient;
    private final PiiCipher piiCipher;
    private final MercadoPagoPreferenceService mercadoPagoPreferenceService;

    public BookingService(BookingRepository bookingRepository,
                          MarcacaoRepository marcacaoRepository,
                          ServicoRepository servicoRepository,
                          CustomerRepository customerRepository,
                          RedissonClient redissonClient,
                          PiiCipher piiCipher,
                          MercadoPagoPreferenceService mercadoPagoPreferenceService) {
        this.bookingRepository = bookingRepository;
        this.marcacaoRepository = marcacaoRepository;
        this.servicoRepository = servicoRepository;
        this.customerRepository = customerRepository;
        this.redissonClient = redissonClient;
        this.piiCipher = piiCipher;
        this.mercadoPagoPreferenceService = mercadoPagoPreferenceService;
    }

    /**
     * Tenta executar uma ação protegida por um lock distribuído no Redis.
     * Utiliza o Redisson para prevenir condições de corrida assíncronas.
     *
     * @param lockKey Chave única para o lock (ex: booking:123 ou slot:2026-02-14)
     * @param waitTime Segundos máximos esperando o lock
     * @param leaseTime Segundos máximos segurando o lock (TTL)
     * @param action A função a ser executada se o lock for adquirido
     * @return true se executou com sucesso, false se o lock já estava tomado.
     */
    public boolean executeWithLock(String lockKey, long waitTime, long leaseTime, Runnable action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        try {
            log.info("Tentando adquirir lock distribuído. chave={} wait={}s lease={}s", lockKey, waitTime, leaseTime);
            isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (isLocked) {
                log.info("Lock distribuído adquirido. chave={}", lockKey);
                action.run();
                return true;
            } else {
                log.warn("Falha ao adquirir lock distribuído. chave={}", lockKey);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrompida enquanto aguardava lock distribuído. chave={}", lockKey, e);
            return false;
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Lock distribuído liberado. chave={}", lockKey);
            }
        }
    }

    @Transactional
    public ResultadoReservaPendente criarReservaPendente(UUID servicoId,
                                                         OffsetDateTime inicioEm,
                                                         Modalidade modalidade,
                                                         String nomeCompleto,
                                                         String email,
                                                         String telefone) {

        Servico servico = buscarServicoPublicadoOuFalhar(servicoId);
        validarModalidade(servico, modalidade);

        OffsetDateTime inicioNormalizado = inicioEm.withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime fimEm = inicioNormalizado.plusMinutes(servico.getDuracaoMinutos());
        String chaveLock = montarChaveLock(servicoId, inicioNormalizado);
        Customer consulente = criarOuAtualizarConsulente(nomeCompleto, email, telefone);
        final Booking[] bookingCriado = new Booking[1];

        try {
            boolean executado = executeWithLock(chaveLock, LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, () -> {
                if (marcacaoRepository.existsOverlappingActiveBooking(inicioNormalizado, fimEm)) {
                    throw new SlotUnavailableException("O horário selecionado já está reservado por outro consulente.");
                }

                OffsetDateTime expiracaoReservaEm = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(HOLD_RESERVA_MINUTOS);

                Booking booking = new Booking(
                        servico,
                        consulente,
                        modalidade,
                        inicioNormalizado,
                        fimEm,
                        BookingStatus.PENDING_PAYMENT,
                        expiracaoReservaEm,
                        servico.getPrecoCentavos()
                );

                bookingCriado[0] = bookingRepository.save(booking);
                log.info(
                        "Booking em hold criado. bookingId={} servicoId={} inicioEm={} holdExpiraEm={}",
                        bookingCriado[0].getId(),
                        servicoId,
                        inicioNormalizado,
                        expiracaoReservaEm
                );
            });

            if (!executado || bookingCriado[0] == null) {
                throw new SlotUnavailableException("Este horário está em processo de reserva por outro consulente.");
            }

            String checkoutUrl = mercadoPagoPreferenceService.criarPreferenciaEObterUrl(bookingCriado[0]);
            return new ResultadoReservaPendente(bookingCriado[0], checkoutUrl);

        } catch (DataIntegrityViolationException excecao) {
            throw new SlotUnavailableException(
                    "O horário selecionado acabou de ser reservado por outro consulente.",
                    excecao
            );
        }
    }

    @Transactional(readOnly = true)
    public Booking buscarPorIdOuFalhar(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Agendamento não encontrado."));
    }

    /**
     * Inicia o processo de checkout e reserva do horário (slot).
     * O critério de aceite exige que seja impossível criar duas reservas para o 
     * mesmo slotId (horário/serviço) enquanto o lock do Redis estiver ativo.
     *
     * @param slotId Identificador único do slot (ex: "serviceId:2026-05-10T15:00:00Z")
     * @param servico O serviço solicitado
     * @param consulente O cliente que está marcando
     * @param modalidade A modalidade escolhida
     * @param inicioEm Início do agendamento
     * @param fimEm Fim do agendamento
     * @return O agendamento criado
     */
    @Transactional
    public Booking initiateCheckout(String slotId, Servico servico, Customer consulente, 
                                    Modalidade modalidade, OffsetDateTime inicioEm, OffsetDateTime fimEm) {
        String chaveLock = montarChaveLock(servico.getId(), inicioEm);
        final Booking[] bookingCriado = new Booking[1];

        boolean executado = executeWithLock(chaveLock, LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, () -> {
            if (marcacaoRepository.existsOverlappingActiveBooking(inicioEm, fimEm)) {
                throw new SlotUnavailableException("O horário selecionado já está reservado por outro consulente.");
            }

            OffsetDateTime expiracaoReservaEm = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(HOLD_RESERVA_MINUTOS);
            Booking booking = new Booking(
                    servico,
                    consulente,
                    modalidade,
                    inicioEm,
                    fimEm,
                    BookingStatus.PENDING_PAYMENT,
                    expiracaoReservaEm,
                    servico.getPrecoCentavos() != null ? servico.getPrecoCentavos() : 0L
            );

            bookingCriado[0] = bookingRepository.save(booking);
        });

        if (!executado || bookingCriado[0] == null) {
            throw new SlotUnavailableException("O horário já está sendo reservado por outro consulente.");
        }

        return bookingCriado[0];
    }
    
    /**
     * Exemplo de processamento de Webhook onde o lock é feito por bookingId
     * conforme a Spec original (workflow gerar-redis-lock).
     */
    @Transactional
    public void processPaymentWebhook(UUID bookingId) {
        String lockKey = "booking:" + bookingId;
        
        boolean executed = executeWithLock(lockKey, 3, 30, () -> {
            Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Agendamento não encontrado."));
                
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setExpiracaoReservaEm(null);
                bookingRepository.save(booking);
            }
        });
        
        if (!executed) {
            throw new ApiException(ErrorCode.EXTERNAL_TIMEOUT, "Não foi possível processar o webhook no momento (Lock timeout).");
        }
    }

    /**
     * Lista as marcações com filtros opcionais.
     * Utilizado pelo painel administrativo (blindado).
     */
    @Transactional(readOnly = true)
    public Page<BookingAdminDto> listarMarcacoesAdmin(BookingStatus status,
                                                      OffsetDateTime dataInicio,
                                                      OffsetDateTime dataFim,
                                                      Pageable pageable) {
        log.info("Buscando marcações administrativas. Status: {}, Inicio: {}, Fim: {}, Página: {}", status, dataInicio, dataFim, pageable);
        return bookingRepository.findAll(criarFiltroMarcacoesAdmin(status, dataInicio, dataFim), pageable)
                .map(BookingAdminDto::fromEntity);
    }

    private Specification<Booking> criarFiltroMarcacoesAdmin(BookingStatus status,
                                                             OffsetDateTime dataInicio,
                                                             OffsetDateTime dataFim) {
        Specification<Booking> specification = Specification.where(null);

        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }

        if (dataInicio != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.<OffsetDateTime>get("inicioEm"), dataInicio));
        }

        if (dataFim != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.<OffsetDateTime>get("inicioEm"), dataFim));
        }

        return specification;
    }

    /**
     * Altera o estado de uma marcação (Agendamento/Tiragem) e adiciona notas administrativas.
     * Segue a regra de negócio centralizada.
     */
    @Transactional
    public void alterarEstadoMarcacao(UUID bookingId, BookingStatus novoStatus, String notasAdmin) {
        String lockKey = "booking:" + bookingId;

        boolean executed = executeWithLock(lockKey, 3, 30, () -> {
            Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Agendamento não encontrado."));

            validarTransicaoAdministrativa(booking.getStatus(), novoStatus);
            log.info("Alterando estado do agendamento {}. Anterior: {}, Novo: {}", bookingId, booking.getStatus(), novoStatus);
            booking.setStatus(novoStatus);

            if (notasAdmin != null && !notasAdmin.isBlank()) {
                booking.setNotasAdmin(notasAdmin);
            }

            if (novoStatus != BookingStatus.PENDING_PAYMENT) {
                booking.setExpiracaoReservaEm(null);
            }

            bookingRepository.save(booking);
        });

        if (!executed) {
            throw new ApiException(ErrorCode.EXTERNAL_TIMEOUT, "Não foi possível alterar o estado da marcação no momento (Lock timeout).");
        }
    }

    private Servico buscarServicoPublicadoOuFalhar(UUID servicoId) {
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Serviço não encontrado."));

        if (!Boolean.TRUE.equals(servico.getPublicado())) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Serviço não está publicado.");
        }

        return servico;
    }

    private void validarModalidade(Servico servico, Modalidade modalidade) {
        Set<Modalidade> modalidades = servico.getModalidades();
        if (modalidade == null || modalidades == null || !modalidades.contains(modalidade)) {
            throw new ApiException(
                    ErrorCode.MODALITY_NOT_ALLOWED_ON_DAY,
                    "A modalidade solicitada não está disponível para este serviço."
            );
        }
    }

    private Customer criarOuAtualizarConsulente(String nomeCompleto, String email, String telefone) {
        String nomeNormalizado = nomeCompleto.trim();
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        String telefoneNormalizado = telefone.trim();
        String emailLookupHash = piiCipher.gerarHashParaBusca(emailNormalizado);

        Customer consulente = customerRepository.findByEmailLookupHash(emailLookupHash)
                .map(existente -> {
                    existente.setNomeCompleto(nomeNormalizado);
                    existente.setEmail(emailNormalizado);
                    existente.setTelefone(telefoneNormalizado);
                    return existente;
                })
                .orElseGet(() -> new Customer(
                        nomeNormalizado,
                        emailNormalizado,
                        telefoneNormalizado,
                        emailLookupHash
                ));

        return customerRepository.save(consulente);
    }

    private String montarChaveLock(UUID servicoId, OffsetDateTime inicioEm) {
        return "lock:booking:" + servicoId + ":" + inicioEm.toInstant();
    }

    private void validarTransicaoAdministrativa(BookingStatus statusAtual, BookingStatus novoStatus) {
        if (statusAtual == novoStatus) {
            return;
        }

        if (statusAtual == BookingStatus.COMPLETED || statusAtual == BookingStatus.REFUNDED) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "O estado atual do agendamento é terminal e não pode ser alterado pelo painel administrativo."
            );
        }

        if ((statusAtual == BookingStatus.EXPIRED || statusAtual == BookingStatus.CANCELLED)
                && novoStatus == BookingStatus.CONFIRMED) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Não é permitido reativar um agendamento expirado ou cancelado como CONFIRMED."
            );
        }
    }
}
