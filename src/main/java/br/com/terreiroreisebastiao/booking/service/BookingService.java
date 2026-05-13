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
import br.com.terreiroreisebastiao.shared.lock.LockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final long HOLD_RESERVA_MINUTOS = 10L;
    private static final long LOCK_WAIT_TIME_SECONDS = 1L;

    public record ResultadoReservaPendente(Booking booking, String checkoutUrl) {}

    private final BookingRepository bookingRepository;
    private final MarcacaoRepository marcacaoRepository;
    private final ServicoRepository servicoRepository;
    private final CustomerRepository customerRepository;
    private final LockService lockService;
    private final PiiCipher piiCipher;
    private final MercadoPagoPreferenceService mercadoPagoPreferenceService;

    public BookingService(BookingRepository bookingRepository,
                          MarcacaoRepository marcacaoRepository,
                          ServicoRepository servicoRepository,
                          CustomerRepository customerRepository,
                          LockService lockService,
                          PiiCipher piiCipher,
                          MercadoPagoPreferenceService mercadoPagoPreferenceService) {
        this.bookingRepository = bookingRepository;
        this.marcacaoRepository = marcacaoRepository;
        this.servicoRepository = servicoRepository;
        this.customerRepository = customerRepository;
        this.lockService = lockService;
        this.piiCipher = piiCipher;
        this.mercadoPagoPreferenceService = mercadoPagoPreferenceService;
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
            boolean executado = lockService.executeWithLock(chaveLock, LOCK_WAIT_TIME_SECONDS, () -> {
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
                log.info("Booking em hold criado. bookingId={} servicoId={} inicioEm={} holdExpiraEm={}",
                        bookingCriado[0].getId(), servicoId, inicioNormalizado, expiracaoReservaEm);
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

    @Transactional
    public Booking initiateCheckout(String slotId, Servico servico, Customer consulente,
                                    Modalidade modalidade, OffsetDateTime inicioEm, OffsetDateTime fimEm) {
        String chaveLock = montarChaveLock(servico.getId(), inicioEm);
        final Booking[] bookingCriado = new Booking[1];

        boolean executado = lockService.executeWithLock(chaveLock, LOCK_WAIT_TIME_SECONDS, () -> {
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

    @Transactional
    public void processPaymentWebhook(UUID bookingId) {
        String lockKey = "booking:" + bookingId;

        boolean executed = lockService.executeWithLock(lockKey, 3, () -> {
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

    @Transactional(readOnly = true)
    public Page<BookingAdminDto> listarMarcacoesAdmin(BookingStatus status,
                                                      OffsetDateTime dataInicio,
                                                      OffsetDateTime dataFim,
                                                      Pageable pageable) {
        log.info("Buscando marcações administrativas. Status: {}, Inicio: {}, Fim: {}, Página: {}", status, dataInicio, dataFim, pageable);
        return bookingRepository.findAll(criarFiltroMarcacoesAdmin(status, dataInicio, dataFim), pageable)
                .map(BookingAdminDto::fromEntity);
    }

    @Transactional
    public void alterarEstadoMarcacao(UUID bookingId, BookingStatus novoStatus, String notasAdmin) {
        String lockKey = "booking:" + bookingId;

        boolean executed = lockService.executeWithLock(lockKey, 3, () -> {
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
