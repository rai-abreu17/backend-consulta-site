package br.com.terreiroreisebastiao.catalog.service;

import br.com.terreiroreisebastiao.booking.repository.BookingRepository;
import br.com.terreiroreisebastiao.catalog.domain.Categoria;
import br.com.terreiroreisebastiao.catalog.domain.Servico;
import br.com.terreiroreisebastiao.catalog.domain.TipoServico;
import br.com.terreiroreisebastiao.catalog.dto.RequisicaoAtualizarServico;
import br.com.terreiroreisebastiao.catalog.dto.RequisicaoCategoria;
import br.com.terreiroreisebastiao.catalog.dto.RequisicaoCriarServico;
import br.com.terreiroreisebastiao.catalog.dto.RespostaCategoria;
import br.com.terreiroreisebastiao.catalog.dto.RespostaServico;
import br.com.terreiroreisebastiao.catalog.repository.CategoriaRepository;
import br.com.terreiroreisebastiao.catalog.repository.ServicoRepository;
import br.com.terreiroreisebastiao.shared.error.ApiException;
import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import br.com.terreiroreisebastiao.shared.error.FieldError;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Serviço de negócio do catálogo — Categorias e Serviços (Consultas e Rituais).
 *
 * <p>Centraliza todas as regras de negócio da Sprint 003 da SPEC:
 * <ul>
 *   <li>CRUD de categorias com validação de vínculos</li>
 *   <li>CRUD de serviços com geração automática de slug e deduplicação</li>
 *   <li>Publicação/despublicação de serviços</li>
 *   <li>Validação de duração (múltiplos de 15), modalidades e tipo</li>
 * </ul>
 *
 * <p><strong>Nenhuma regra de negócio reside nos Controllers.</strong>
 * Este serviço é a fonte única de verdade para a lógica do catálogo.</p>
 */
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoService.class);

    private static final short DURACAO_MINIMA = 15;
    private static final short DURACAO_MAXIMA = 480;
    private static final short MULTIPLO_DURACAO = 15;

    private static final Pattern PADRAO_SLUG = Pattern.compile("[^a-z0-9]+");
    private static final Pattern PADRAO_ACENTOS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final CategoriaRepository categoriaRepository;
    private final ServicoRepository servicoRepository;
    private final BookingRepository bookingRepository;

    // ══════════════════════════════════════════════
    //  CATEGORIAS
    // ══════════════════════════════════════════════

    /**
     * Lista todas as categorias publicadas (visão pública).
     *
     * @return lista ordenada por posição de exibição
     */
    @Transactional(readOnly = true)
    public List<RespostaCategoria> listarCategoriasPublicadas() {
        return categoriaRepository.findByPublicadaTrueOrderByOrdemExibicaoAsc()
                .stream()
                .map(RespostaCategoria::de)
                .toList();
    }

    /**
     * Lista todas as categorias (visão admin).
     *
     * @return lista ordenada por posição de exibição
     */
    @Transactional(readOnly = true)
    public List<RespostaCategoria> listarTodasCategorias() {
        return categoriaRepository.findAllByOrderByOrdemExibicaoAsc()
                .stream()
                .map(RespostaCategoria::de)
                .toList();
    }

    /**
     * Busca uma categoria por ID na visão administrativa.
     */
    @Transactional(readOnly = true)
    public RespostaCategoria buscarCategoria(UUID categoriaId) {
        return RespostaCategoria.de(buscarCategoriaOuFalhar(categoriaId));
    }

    /**
     * Cria uma nova categoria no catálogo.
     *
     * @param requisicao dados da nova categoria
     * @return DTO da categoria criada
     */
    @Transactional
    public RespostaCategoria criarCategoria(RequisicaoCategoria requisicao) {
        String nomeTrimado = requisicao.nome().trim();
        String slug = gerarSlugUnicoCategoria(nomeTrimado);
        Short ordem = requisicao.ordemExibicao() != null ? requisicao.ordemExibicao() : 0;

        Categoria categoria = new Categoria(slug, nomeTrimado, requisicao.descricao(), ordem);
        categoria = categoriaRepository.save(categoria);

        log.info("Categoria criada. id={} slug={}", categoria.getId(), slug);
        return RespostaCategoria.de(categoria);
    }

    /**
     * Atualiza uma categoria existente.
     *
     * @param categoriaId UUID da categoria a atualizar
     * @param requisicao  novos dados
     * @return DTO da categoria atualizada
     */
    @Transactional
    public RespostaCategoria atualizarCategoria(UUID categoriaId, RequisicaoCategoria requisicao) {
        Categoria categoria = buscarCategoriaOuFalhar(categoriaId);

        String nomeTrimado = requisicao.nome().trim();
        categoria.setNome(nomeTrimado);
        categoria.setDescricao(requisicao.descricao());

        if (requisicao.ordemExibicao() != null) {
            categoria.setOrdemExibicao(requisicao.ordemExibicao());
        }

        categoria = categoriaRepository.save(categoria);
        log.info("Categoria atualizada. id={}", categoriaId);
        return RespostaCategoria.de(categoria);
    }

    /**
     * Remove uma categoria. Restrição: não pode ter serviços vinculados.
     *
     * @param categoriaId UUID da categoria a remover
     */
    @Transactional
    public void removerCategoria(UUID categoriaId) {
        Categoria categoria = buscarCategoriaOuFalhar(categoriaId);

        if (servicoRepository.existsByCategoriaId(categoriaId)) {
            throw new ApiException(ErrorCode.CATEGORY_HAS_SERVICES,
                    "A categoria '" + categoria.getNome() + "' possui serviços vinculados e não pode ser removida.");
        }

        categoriaRepository.delete(categoria);
        log.info("Categoria removida. id={}", categoriaId);
    }

    // ══════════════════════════════════════════════
    //  SERVIÇOS
    // ══════════════════════════════════════════════

    /**
     * Lista serviços publicados com filtros opcionais (visão pública).
     *
     * @param tipo          tipo de serviço (ou {@code null})
     * @param categoriaSlug slug da categoria (ou {@code null})
     * @param pageable      configuração de paginação
     * @return página de serviços publicados
     */
    @Transactional(readOnly = true)
    public Page<RespostaServico> listarServicosPublicados(TipoServico tipo,
                                                          String categoriaSlug,
                                                          Pageable pageable) {
        return servicoRepository.listarPublicados(tipo, categoriaSlug, pageable)
                .map(RespostaServico::de);
    }

    /**
     * Lista todos os serviços com filtros opcionais (visão admin).
     *
     * @param tipo        tipo de serviço (ou {@code null})
     * @param categoriaId UUID da categoria (ou {@code null})
     * @param pageable    configuração de paginação
     * @return página de serviços
     */
    @Transactional(readOnly = true)
    public Page<RespostaServico> listarTodosServicos(TipoServico tipo,
                                                     UUID categoriaId,
                                                     Pageable pageable) {
        return servicoRepository.listarTodos(tipo, categoriaId, pageable)
                .map(RespostaServico::de);
    }

    /**
     * Busca um serviço por ID na visão administrativa.
     */
    @Transactional(readOnly = true)
    public RespostaServico buscarServico(UUID servicoId) {
        return RespostaServico.de(buscarServicoOuFalhar(servicoId));
    }

    /**
     * Busca um serviço publicado pelo slug (visão pública).
     *
     * @param slug identificador amigável
     * @return DTO do serviço
     */
    @Transactional(readOnly = true)
    public RespostaServico buscarServicoPublicadoPorSlug(String slug) {
        Servico servico = servicoRepository.findBySlugAndPublicadoTrue(slug)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Serviço não encontrado ou não publicado."));
        return RespostaServico.de(servico);
    }

    /**
     * Cria um novo serviço no catálogo.
     *
     * <p>Validações de negócio aplicadas:
     * <ul>
     *   <li>Duração deve ser múltiplo de 15</li>
     *   <li>Tipo deve ser válido (CONSULTATION ou RITUAL)</li>
     *   <li>Categoria deve existir</li>
     *   <li>Slug gerado automaticamente com deduplicação</li>
     * </ul>
     *
     * @param requisicao dados do novo serviço
     * @return DTO do serviço criado
     */
    @Transactional
    public RespostaServico criarServico(RequisicaoCriarServico requisicao) {
        validarDuracao(requisicao.duracaoMinutos());
        validarPrecoCentavos(requisicao.precoCentavos());

        TipoServico tipo = parseTipoServico(requisicao.type());
        Categoria categoria = buscarCategoriaOuFalhar(requisicao.categoriaId());

        String nomeTrimado = requisicao.nome().trim();
        String slug = resolverSlugCriacao(requisicao.slug(), nomeTrimado);

        Servico servico = new Servico(
                categoria, tipo, slug, nomeTrimado,
                requisicao.descricaoCurta(), requisicao.descricaoLonga(),
                requisicao.duracaoMinutos(), requisicao.precoCentavos(),
                requisicao.modalities()
        );

        servico = servicoRepository.save(servico);

        log.info("Serviço criado. id={} slug={} tipo={}", servico.getId(), slug, tipo);
        return RespostaServico.de(servico);
    }

    /**
     * Atualiza parcialmente um serviço existente (PATCH semântico).
     *
     * @param servicoId  UUID do serviço a atualizar
     * @param requisicao campos a atualizar (apenas os informados)
     * @return DTO do serviço atualizado
     */
    @Transactional
    public RespostaServico atualizarServico(UUID servicoId,
                                             RequisicaoAtualizarServico requisicao) {
        Servico servico = buscarServicoOuFalhar(servicoId);

        if (requisicao.categoriaId() != null) {
            Categoria novaCategoria = buscarCategoriaOuFalhar(requisicao.categoriaId());
            servico.setCategoria(novaCategoria);
        }

        if (requisicao.slug() != null) {
            servico.setSlug(resolverSlugAtualizacao(requisicao.slug(), servico.getId()));
        }

        if (requisicao.nome() != null) {
            servico.setNome(requisicao.nome().trim());
        }

        if (requisicao.descricaoCurta() != null) {
            servico.setDescricaoCurta(requisicao.descricaoCurta());
        }

        if (requisicao.descricaoLonga() != null) {
            servico.setDescricaoLonga(requisicao.descricaoLonga());
        }

        if (requisicao.duracaoMinutos() != null) {
            validarDuracao(requisicao.duracaoMinutos());
            servico.setDuracaoMinutos(requisicao.duracaoMinutos());
        }

        if (requisicao.precoCentavos() != null) {
            validarPrecoCentavos(requisicao.precoCentavos());
            servico.setPrecoCentavos(requisicao.precoCentavos());
        }

        if (requisicao.modalities() != null && !requisicao.modalities().isEmpty()) {
            servico.setModalidades(requisicao.modalities());
        }

        if (requisicao.ordemExibicao() != null) {
            servico.setOrdemExibicao(requisicao.ordemExibicao());
        }

        servico = servicoRepository.save(servico);
        log.info("Serviço atualizado. id={}", servicoId);
        return RespostaServico.de(servico);
    }

    /**
     * Substitui integralmente um serviço existente.
     */
    @Transactional
    public RespostaServico substituirServico(UUID servicoId,
                                             RequisicaoCriarServico requisicao) {
        validarDuracao(requisicao.duracaoMinutos());
        validarPrecoCentavos(requisicao.precoCentavos());

        Servico servico = buscarServicoOuFalhar(servicoId);
        Categoria categoria = buscarCategoriaOuFalhar(requisicao.categoriaId());
        TipoServico tipo = parseTipoServico(requisicao.type());
        String slug = temTexto(requisicao.slug())
            ? resolverSlugAtualizacao(requisicao.slug(), servicoId)
            : servico.getSlug();

        servico.setCategoria(categoria);
        servico.setTipo(tipo);
        servico.setSlug(slug);
        servico.setNome(requisicao.nome().trim());
        servico.setDescricaoCurta(requisicao.descricaoCurta());
        servico.setDescricaoLonga(requisicao.descricaoLonga());
        servico.setDuracaoMinutos(requisicao.duracaoMinutos());
        servico.setPrecoCentavos(requisicao.precoCentavos());
        servico.setModalidades(requisicao.modalities());

        servico = servicoRepository.save(servico);
        log.info("Serviço substituído. id={} tipo={}", servicoId, tipo);
        return RespostaServico.de(servico);
    }

    /**
     * Publica um serviço, tornando-o visível ao público.
     *
     * @param servicoId UUID do serviço
     * @return DTO do serviço atualizado
     */
    @Transactional
    public RespostaServico publicarServico(UUID servicoId) {
        Servico servico = buscarServicoOuFalhar(servicoId);
        servico.setPublicado(true);
        servico = servicoRepository.save(servico);

        log.info("Serviço publicado. id={} slug={}", servicoId, servico.getSlug());
        return RespostaServico.de(servico);
    }

    /**
     * Despublica um serviço, removendo-o da visão pública.
     *
     * @param servicoId UUID do serviço
     * @return DTO do serviço atualizado
     */
    @Transactional
    public RespostaServico despublicarServico(UUID servicoId) {
        Servico servico = buscarServicoOuFalhar(servicoId);
        servico.setPublicado(false);
        servico = servicoRepository.save(servico);

        log.info("Serviço despublicado. id={} slug={}", servicoId, servico.getSlug());
        return RespostaServico.de(servico);
    }

    /**
     * Remove um serviço sem deixar a decisão de integridade apenas nas bordas HTTP.
     */
    @Transactional
    public void removerServico(UUID servicoId) {
        Servico servico = buscarServicoOuFalhar(servicoId);

        if (bookingRepository.existsByServicoId(servicoId)) {
            throw new ApiException(
                    ErrorCode.SERVICE_HAS_FUTURE_BOOKINGS,
                    "O serviço '" + servico.getNome() + "' possui marcações vinculadas e não pode ser removido agora."
            );
        }

        servicoRepository.delete(servico);
        log.info("Serviço removido. id={}", servicoId);
    }

    // ══════════════════════════════════════════════
    //  MÉTODOS INTERNOS
    // ══════════════════════════════════════════════

    private Categoria buscarCategoriaOuFalhar(UUID categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Categoria não encontrada."));
    }

    private Servico buscarServicoOuFalhar(UUID servicoId) {
        return servicoRepository.buscarPorIdComCategoria(servicoId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Serviço não encontrado."));
    }

    /**
     * Valida que a duração é múltiplo de 15 dentro do intervalo permitido.
     */
    private void validarDuracao(Short duracao) {
        if (duracao == null) {
            throw new ApiException(
                ErrorCode.VALIDATION_ERROR,
                "A duração do serviço é obrigatória.",
                List.of(new FieldError("durationMin", "Informe uma duração entre 15 e 480 minutos."))
            );
        }

        if (duracao < DURACAO_MINIMA || duracao > DURACAO_MAXIMA) {
            throw new ApiException(
                ErrorCode.VALIDATION_ERROR,
                "A duração informada está fora do intervalo aceito.",
                List.of(new FieldError(
                    "durationMin",
                    "A duração deve ficar entre 15 e 480 minutos."
                ))
            );
        }

        if (duracao % MULTIPLO_DURACAO != 0) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "A duração deve ser um múltiplo de 15 minutos.",
                    List.of(new FieldError("durationMin",
                            "A duração deve ser um múltiplo de 15 minutos (ex.: 15, 30, 45, 60...)."))
            );
        }
    }

        private void validarPrecoCentavos(Long precoCentavos) {
        if (precoCentavos == null || precoCentavos < 0) {
            throw new ApiException(
                ErrorCode.VALIDATION_ERROR,
                "O valor do serviço deve ser informado em centavos e não pode ser negativo.",
                List.of(new FieldError(
                    "priceCents",
                    "Informe um valor maior ou igual a zero em centavos."
                ))
            );
        }
        }

    private String resolverSlugCriacao(String slugInformado, String nomeFallback) {
        if (temTexto(slugInformado)) {
            String slugNormalizado = normalizarSlugOuFalhar(slugInformado);

            if (servicoRepository.existsBySlug(slugNormalizado)) {
                throw criarErroSlugDuplicado();
            }

            return slugNormalizado;
        }

        return gerarSlugUnicoServico(nomeFallback);
    }

    private String resolverSlugAtualizacao(String slugInformado, UUID servicoId) {
        String slugNormalizado = normalizarSlugOuFalhar(slugInformado);

        if (servicoRepository.existsBySlugAndIdNot(slugNormalizado, servicoId)) {
            throw criarErroSlugDuplicado();
        }

        return slugNormalizado;
    }

    private String normalizarSlugOuFalhar(String valorOriginal) {
        String slugNormalizado = gerarSlug(valorOriginal);

        if (!temTexto(slugNormalizado)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "O slug informado é inválido.",
                    List.of(new FieldError(
                            "slug",
                            "Informe um slug com letras minúsculas, números e hífens."
                    ))
            );
        }

        return slugNormalizado;
    }

    private ApiException criarErroSlugDuplicado() {
        return new ApiException(
                ErrorCode.VALIDATION_ERROR,
                "O slug informado já está em uso por outro serviço.",
                List.of(new FieldError(
                        "slug",
                        "Escolha um slug diferente para este serviço."
                ))
        );
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    /**
     * Faz o parsing seguro do tipo de serviço a partir da string recebida.
     */
    private TipoServico parseTipoServico(String tipo) {
        try {
            return TipoServico.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException excecao) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Tipo de serviço inválido. Valores aceitos: CONSULTATION, RITUAL.",
                    List.of(new FieldError("type",
                            "Tipo de serviço inválido. Valores aceitos: CONSULTATION, RITUAL."))
            );
        }
    }

    /**
     * Gera um slug a partir do nome e garante unicidade na tabela de categorias.
     *
     * <p>Remove acentos, converte para minúsculas, substitui caracteres
     * não alfanuméricos por hífen. Conflitos são resolvidos com sufixo
     * incremental (-2, -3, ...).</p>
     *
     * @param nome nome de origem
     * @return slug único
     */
    private String gerarSlugUnicoCategoria(String nome) {
        String slugBase = gerarSlug(nome);

        if (!categoriaRepository.existsBySlug(slugBase)) {
            return slugBase;
        }

        return encontrarSlugComSufixo(slugBase, categoriaRepository::existsBySlug);
    }

    /**
     * Gera um slug a partir do nome e garante unicidade na tabela de serviços.
     *
     * @param nome nome de origem
     * @return slug único
     */
    private String gerarSlugUnicoServico(String nome) {
        String slugBase = gerarSlug(nome);

        if (!servicoRepository.existsBySlug(slugBase)) {
            return slugBase;
        }

        return encontrarSlugComSufixo(slugBase, servicoRepository::existsBySlug);
    }

    /**
     * Converte um nome para formato slug (lowercase, sem acentos, hifenizado).
     */
    private String gerarSlug(String nome) {
        String normalizado = Normalizer.normalize(nome, Normalizer.Form.NFD);
        String semAcentos = PADRAO_ACENTOS.matcher(normalizado).replaceAll("");
        String slug = PADRAO_SLUG.matcher(semAcentos.toLowerCase(Locale.ROOT)).replaceAll("-");
        return slug.replaceAll("^-|-$", "");
    }

    /**
     * Encontra um slug disponível adicionando sufixo incremental.
     */
    private String encontrarSlugComSufixo(String slugBase,
                                           java.util.function.Predicate<String> existeSlug) {
        int sufixo = 2;
        String slugTentativa;
        do {
            slugTentativa = slugBase + "-" + sufixo;
            sufixo++;
        } while (existeSlug.test(slugTentativa));
        return slugTentativa;
    }
}
