package br.com.terreiroreisebastiao.availability.service;

import br.com.terreiroreisebastiao.availability.domain.ExcecaoDia;
import br.com.terreiroreisebastiao.availability.domain.RegraDisponibilidade;
import br.com.terreiroreisebastiao.availability.dto.ExcecaoDiaDto;
import br.com.terreiroreisebastiao.availability.dto.RegraDisponibilidadeDto;
import br.com.terreiroreisebastiao.availability.dto.SalvarExcecaoDiaRequest;
import br.com.terreiroreisebastiao.availability.dto.SalvarRegraDisponibilidadeRequest;
import br.com.terreiroreisebastiao.availability.repository.ExcecaoDiaRepository;
import br.com.terreiroreisebastiao.availability.repository.RegraDisponibilidadeRepository;
import br.com.terreiroreisebastiao.shared.error.ApiException;
import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Comparator;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final RegraDisponibilidadeRepository regraRepository;
    private final ExcecaoDiaRepository excecaoRepository;

    @Transactional(readOnly = true)
    public List<RegraDisponibilidadeDto> buscarRegrasSemanais() {
        return regraRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(RegraDisponibilidade::getWeekday)
                        .thenComparing(RegraDisponibilidade::getHoraInicio))
                .map(this::mapearRegraParaDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RegraDisponibilidadeDto criarRegraSemanal(SalvarRegraDisponibilidadeRequest requisicao) {
        log.info("Criando nova regra semanal de disponibilidade para weekday={}", requisicao.weekday());
        validarRegra(requisicao.weekday(), requisicao.startTime(), requisicao.endTime(), requisicao.modalities());

        RegraDisponibilidade entidade = new RegraDisponibilidade(
                requisicao.weekday(),
                requisicao.startTime(),
                requisicao.endTime(),
                serializarModalidades(requisicao.modalities()),
                resolverAtivo(requisicao.isActive())
        );

        return mapearRegraParaDto(regraRepository.save(entidade));
    }

    @Transactional
    public RegraDisponibilidadeDto atualizarRegraSemanal(UUID regraId, SalvarRegraDisponibilidadeRequest requisicao) {
        log.info("Atualizando regra semanal de disponibilidade. regraId={}", regraId);
        validarRegra(requisicao.weekday(), requisicao.startTime(), requisicao.endTime(), requisicao.modalities());

        RegraDisponibilidade entidade = regraRepository.findById(regraId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Regra semanal não encontrada."));

        entidade.atualizar(
                requisicao.weekday(),
                requisicao.startTime(),
                requisicao.endTime(),
                serializarModalidades(requisicao.modalities()),
                resolverAtivo(requisicao.isActive())
        );

        return mapearRegraParaDto(regraRepository.save(entidade));
    }

    @Transactional
    public void removerRegraSemanal(UUID regraId) {
        log.info("Removendo regra semanal de disponibilidade. regraId={}", regraId);
        RegraDisponibilidade entidade = regraRepository.findById(regraId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Regra semanal não encontrada."));
        regraRepository.delete(entidade);
    }

    @Transactional(readOnly = true)
    public List<ExcecaoDiaDto> buscarExcecoes(LocalDate de, LocalDate ate) {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "A data inicial deve ser anterior ou igual à data final.");
        }
        
        if (de == null || ate == null) {
            return excecaoRepository.findAll().stream()
                    .sorted(Comparator.comparing(ExcecaoDia::getData))
                    .map(this::mapearExcecaoParaDto)
                    .collect(Collectors.toList());
        }
        
        return excecaoRepository.findByDataBetween(de, ate).stream()
                .sorted(Comparator.comparing(ExcecaoDia::getData))
                .map(this::mapearExcecaoParaDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExcecaoDiaDto criarExcecaoDia(SalvarExcecaoDiaRequest requisicao) {
        log.info("Criando nova exceção de agenda para o dia {}", requisicao.date());
        validarExcecao(requisicao.date(), requisicao.isClosed(), requisicao.startTime(), requisicao.endTime(), requisicao.modalities());

        if (excecaoRepository.findByData(requisicao.date()).isPresent()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.CONFLICT,
                    "Já existe uma exceção registrada para esta data."
            );
        }

        ExcecaoDia excecao = new ExcecaoDia(
                requisicao.date(),
                requisicao.isClosed(),
                normalizarHorarioSeFechado(requisicao.isClosed(), requisicao.startTime()),
                normalizarHorarioSeFechado(requisicao.isClosed(), requisicao.endTime()),
                serializarModalidadesSeAberto(requisicao.isClosed(), requisicao.modalities()),
                requisicao.reason()
        );

        return mapearExcecaoParaDto(excecaoRepository.save(excecao));
    }

    @Transactional
    public ExcecaoDiaDto atualizarExcecaoDia(UUID excecaoId, SalvarExcecaoDiaRequest requisicao) {
        log.info("Atualizando exceção de agenda. excecaoId={}", excecaoId);
        validarExcecao(requisicao.date(), requisicao.isClosed(), requisicao.startTime(), requisicao.endTime(), requisicao.modalities());

        ExcecaoDia excecao = excecaoRepository.findById(excecaoId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Exceção de agenda não encontrada."));

        if (excecaoRepository.existsByDataAndIdNot(requisicao.date(), excecaoId)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.CONFLICT,
                    "Já existe uma exceção registrada para a data informada."
            );
        }

        excecao.atualizar(
                requisicao.date(),
                requisicao.isClosed(),
                normalizarHorarioSeFechado(requisicao.isClosed(), requisicao.startTime()),
                normalizarHorarioSeFechado(requisicao.isClosed(), requisicao.endTime()),
                serializarModalidadesSeAberto(requisicao.isClosed(), requisicao.modalities()),
                requisicao.reason()
        );

        return mapearExcecaoParaDto(excecaoRepository.save(excecao));
    }

    @Transactional
    public void removerExcecaoDia(UUID excecaoId) {
        log.info("Removendo exceção de agenda. excecaoId={}", excecaoId);
        ExcecaoDia excecao = excecaoRepository.findById(excecaoId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Exceção de agenda não encontrada."));
        excecaoRepository.delete(excecao);
    }

    private void validarRegra(Short weekday, LocalTime horaInicio, LocalTime horaFim, java.util.Set<?> modalidades) {
        if (weekday < 0 || weekday > 6) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "O dia da semana deve ser entre 0 e 6.");
        }
        if (!horaInicio.isBefore(horaFim)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "O horário de início deve ser anterior ao horário de término.");
        }
        if (modalidades == null || modalidades.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Selecione pelo menos uma modalidade para a regra semanal.");
        }
    }

    private void validarExcecao(LocalDate data, Boolean fechado, LocalTime horaInicio, LocalTime horaFim, java.util.Set<?> modalidades) {
        if (data == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "A data da exceção é obrigatória.");
        }
        if (!Boolean.TRUE.equals(fechado)) {
            if (horaInicio == null || horaFim == null) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Horário de início e fim são obrigatórios para exceções abertas.");
            }
            if (!horaInicio.isBefore(horaFim)) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "O horário de início deve ser anterior ao horário de término.");
            }
            if (modalidades == null || modalidades.isEmpty()) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Selecione pelo menos uma modalidade para a exceção aberta.");
            }
        }
    }

    private Boolean resolverAtivo(Boolean ativo) {
        return ativo != null ? ativo : Boolean.TRUE;
    }

    private LocalTime normalizarHorarioSeFechado(Boolean fechado, LocalTime horario) {
        return Boolean.TRUE.equals(fechado) ? null : horario;
    }

    private String serializarModalidades(java.util.Set<?> modalidades) {
        return modalidades.stream()
                .map(Object::toString)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private String serializarModalidadesSeAberto(Boolean fechado, java.util.Set<?> modalidades) {
        if (Boolean.TRUE.equals(fechado)) {
            return null;
        }

        return serializarModalidades(modalidades);
    }

    private RegraDisponibilidadeDto mapearRegraParaDto(RegraDisponibilidade entidade) {
        return new RegraDisponibilidadeDto(
                entidade.getId(),
                entidade.getWeekday(),
                entidade.getHoraInicio(),
                entidade.getHoraFim(),
                entidade.getModalidadesAsSet(),
                entidade.getAtivo()
        );
    }

    private ExcecaoDiaDto mapearExcecaoParaDto(ExcecaoDia entidade) {
        return new ExcecaoDiaDto(
                entidade.getId(),
                entidade.getData(),
                entidade.getFechado(),
                entidade.getHoraInicio(),
                entidade.getHoraFim(),
                entidade.getModalidadesAsSet(),
                entidade.getMotivo()
        );
    }
}
