package br.com.terreiroreisebastiao.availability.service;

import br.com.terreiroreisebastiao.availability.domain.ExcecaoDia;
import br.com.terreiroreisebastiao.availability.domain.RegraDisponibilidade;
import br.com.terreiroreisebastiao.availability.dto.SlotDto;
import br.com.terreiroreisebastiao.availability.repository.ExcecaoDiaRepository;
import br.com.terreiroreisebastiao.availability.repository.RegraDisponibilidadeRepository;
import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.booking.repository.MarcacaoRepository;
import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import br.com.terreiroreisebastiao.catalog.domain.Servico;
import br.com.terreiroreisebastiao.catalog.repository.ServicoRepository;
import br.com.terreiroreisebastiao.shared.error.ApiException;
import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SlotService {

    private static final ZoneId FUSO_CANONICO = ZoneId.of("America/Fortaleza");
    private static final ZoneOffset FUSO_SAIDA = ZoneOffset.UTC;
    private static final long LIMITE_MAXIMO_DIAS = 60L;

    private final ServicoRepository servicoRepository;
    private final RegraDisponibilidadeRepository regraRepository;
    private final ExcecaoDiaRepository excecaoRepository;
    private final MarcacaoRepository marcacaoRepository;

    public SlotService(ServicoRepository servicoRepository,
                       RegraDisponibilidadeRepository regraRepository,
                       ExcecaoDiaRepository excecaoRepository,
                       MarcacaoRepository marcacaoRepository) {
        this.servicoRepository = servicoRepository;
        this.regraRepository = regraRepository;
        this.excecaoRepository = excecaoRepository;
        this.marcacaoRepository = marcacaoRepository;
    }

    public List<SlotDto> gerarSlots(LocalDate data, UUID servicoId) {
        return gerarSlots(servicoId, data, data.plusDays(1), "ANY");
    }

    public List<SlotDto> gerarSlots(UUID serviceId, LocalDate from, LocalDate to, String modalityStr) {
        validarIntervalo(from, to);

        Servico servico = buscarServicoPublicadoOuFalhar(serviceId);
        Modalidade modalidadeSolicitada = parseModalidade(modalityStr);
        Map<Short, List<RegraDisponibilidade>> regrasPorDia = carregarRegrasAtivasPorDia();
        Map<LocalDate, ExcecaoDia> excecoesPorData = carregarExcecoesPorData(from, to);
        List<IntervaloOcupado> intervalosOcupados = carregarIntervalosOcupados(from, to);
        ZonedDateTime limiteMinimo = ZonedDateTime.now(FUSO_CANONICO).plusHours(1);
        long duracaoSlotEmMinutos = servico.getDuracaoMinutos();

        List<SlotDto> slots = new ArrayList<>();

        for (LocalDate data = from; data.isBefore(to); data = data.plusDays(1)) {
            for (JanelaDeTempo janela : resolverJanelasDoDia(data, regrasPorDia, excecoesPorData)) {
                ZonedDateTime cursor = ZonedDateTime.of(data, janela.inicio(), FUSO_CANONICO);
                ZonedDateTime fimJanela = ZonedDateTime.of(data, janela.fim(), FUSO_CANONICO);

                while (!cursor.plusMinutes(duracaoSlotEmMinutos).isAfter(fimJanela)) {
                    ZonedDateTime inicioSlotLocal = cursor;
                    ZonedDateTime fimSlotLocal = cursor.plusMinutes(duracaoSlotEmMinutos);
                    Set<Modalidade> modalidadesDisponiveis = calcularModalidadesDisponiveis(
                            janela.modalidades(),
                            servico.getModalidades(),
                            modalidadeSolicitada
                    );

                    if (!modalidadesDisponiveis.isEmpty() && !inicioSlotLocal.isBefore(limiteMinimo)) {
                        OffsetDateTime inicioSlotUtc = converterParaUtc(inicioSlotLocal);
                        OffsetDateTime fimSlotUtc = converterParaUtc(fimSlotLocal);

                        if (!existeSobreposicao(intervalosOcupados, inicioSlotUtc, fimSlotUtc)) {
                            slots.add(new SlotDto(inicioSlotUtc, fimSlotUtc, modalidadesDisponiveis));
                        }
                    }

                    cursor = fimSlotLocal;
                }
            }
        }

        if (slots.isEmpty() && modalidadeSolicitada != null) {
            throw new ApiException(
                    ErrorCode.MODALITY_NOT_ALLOWED_ON_DAY,
                    "Nenhum slot encontrado para a modalidade solicitada neste intervalo."
            );
        }

        return slots;
    }

    private void validarIntervalo(LocalDate from, LocalDate to) {
        if (!from.isBefore(to)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "A data final deve ser posterior à data inicial."
            );
        }

        if (ChronoUnit.DAYS.between(from, to) > LIMITE_MAXIMO_DIAS) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "O intervalo máximo de busca é de 60 dias."
            );
        }
    }

    private Servico buscarServicoPublicadoOuFalhar(UUID servicoId) {
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Serviço não encontrado."
                ));

        if (!Boolean.TRUE.equals(servico.getPublicado())) {
            throw new ApiException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Serviço não está publicado."
            );
        }

        return servico;
    }

    private Modalidade parseModalidade(String modalidadeTexto) {
        if (modalidadeTexto == null || modalidadeTexto.isBlank() || "ANY".equalsIgnoreCase(modalidadeTexto)) {
            return null;
        }

        try {
            return Modalidade.valueOf(modalidadeTexto.trim().toUpperCase());
        } catch (IllegalArgumentException excecao) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Modalidade inválida. Valores aceitos: ONLINE, IN_PERSON ou ANY."
            );
        }
    }

    private Map<Short, List<RegraDisponibilidade>> carregarRegrasAtivasPorDia() {
        return regraRepository.findAll().stream()
                .filter(regra -> Boolean.TRUE.equals(regra.getAtivo()))
                .sorted(Comparator.comparing(RegraDisponibilidade::getHoraInicio))
                .collect(Collectors.groupingBy(RegraDisponibilidade::getWeekday));
    }

    private Map<LocalDate, ExcecaoDia> carregarExcecoesPorData(LocalDate from, LocalDate to) {
        return excecaoRepository.findByDataBetween(from, to.minusDays(1)).stream()
                .collect(Collectors.toMap(ExcecaoDia::getData, Function.identity()));
    }

    private List<IntervaloOcupado> carregarIntervalosOcupados(LocalDate from, LocalDate to) {
        OffsetDateTime inicioUtc = from.atStartOfDay(FUSO_CANONICO)
                .withZoneSameInstant(FUSO_SAIDA)
                .toOffsetDateTime();
        OffsetDateTime fimUtc = to.atStartOfDay(FUSO_CANONICO)
                .withZoneSameInstant(FUSO_SAIDA)
                .toOffsetDateTime();

        return marcacaoRepository.listarMarcacoesAtivasNoIntervalo(inicioUtc, fimUtc).stream()
                .map(this::mapearIntervaloOcupado)
                .toList();
    }

    private IntervaloOcupado mapearIntervaloOcupado(Booking booking) {
        return new IntervaloOcupado(booking.getInicioEm(), booking.getFimEm());
    }

    private List<JanelaDeTempo> resolverJanelasDoDia(
            LocalDate data,
            Map<Short, List<RegraDisponibilidade>> regrasPorDia,
            Map<LocalDate, ExcecaoDia> excecoesPorData
    ) {
        ExcecaoDia excecao = excecoesPorData.get(data);

        if (excecao != null && Boolean.TRUE.equals(excecao.getFechado())) {
            return List.of();
        }

        if (excecao != null && excecao.getHoraInicio() != null && excecao.getHoraFim() != null) {
            return List.of(new JanelaDeTempo(
                    excecao.getHoraInicio(),
                    excecao.getHoraFim(),
                    excecao.getModalidadesAsSet()
            ));
        }

        short weekday = (short) (data.getDayOfWeek().getValue() % 7);

        return regrasPorDia.getOrDefault(weekday, Collections.emptyList()).stream()
                .map(regra -> new JanelaDeTempo(
                        regra.getHoraInicio(),
                        regra.getHoraFim(),
                        regra.getModalidadesAsSet()
                ))
                .toList();
    }

    private Set<Modalidade> calcularModalidadesDisponiveis(
            Set<Modalidade> modalidadesJanela,
            Set<Modalidade> modalidadesServico,
            Modalidade modalidadeSolicitada
    ) {
        EnumSet<Modalidade> modalidadesEfetivas = modalidadesJanela == null || modalidadesJanela.isEmpty()
                ? EnumSet.noneOf(Modalidade.class)
                : EnumSet.copyOf(modalidadesJanela);

        Set<Modalidade> modalidadesDoServico = modalidadesServico == null || modalidadesServico.isEmpty()
                ? EnumSet.noneOf(Modalidade.class)
                : EnumSet.copyOf(modalidadesServico);

        modalidadesEfetivas.retainAll(modalidadesDoServico);

        if (modalidadeSolicitada != null) {
            modalidadesEfetivas.retainAll(EnumSet.of(modalidadeSolicitada));
        }

        return Set.copyOf(modalidadesEfetivas);
    }

    private boolean existeSobreposicao(
            List<IntervaloOcupado> intervalosOcupados,
            OffsetDateTime inicioSlotUtc,
            OffsetDateTime fimSlotUtc
    ) {
        return intervalosOcupados.stream().anyMatch(intervalo ->
                intervalo.inicio().isBefore(fimSlotUtc)
                        && intervalo.fim().isAfter(inicioSlotUtc)
        );
    }

    private OffsetDateTime converterParaUtc(ZonedDateTime instanteLocal) {
        return instanteLocal.withZoneSameInstant(FUSO_SAIDA).toOffsetDateTime();
    }

    private record JanelaDeTempo(LocalTime inicio, LocalTime fim, Set<Modalidade> modalidades) {
    }

    private record IntervaloOcupado(OffsetDateTime inicio, OffsetDateTime fim) {
    }
}
