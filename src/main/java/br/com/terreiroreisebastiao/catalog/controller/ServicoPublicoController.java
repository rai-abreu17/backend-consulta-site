package br.com.terreiroreisebastiao.catalog.controller;

import br.com.terreiroreisebastiao.catalog.domain.TipoServico;
import br.com.terreiroreisebastiao.catalog.dto.RespostaServico;
import br.com.terreiroreisebastiao.catalog.service.CatalogoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST público para consulta de serviços do catálogo.
 *
 * <p>Acessível sem autenticação. Retorna apenas serviços publicados.</p>
 *
 * @see CatalogoService
 */
@RestController
@RequestMapping("/api/v1/catalog/services")
public class ServicoPublicoController {

    private final CatalogoService catalogoService;

    public ServicoPublicoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /**
     * {@code GET /api/v1/catalog/services} — Lista serviços publicados.
     *
     * <p>Suporta filtros opcionais por tipo e slug da categoria.</p>
     *
     * @param type         tipo do serviço (CONSULTATION ou RITUAL) — opcional
     * @param categorySlug slug da categoria — opcional
     * @param pageable     configuração de paginação
     * @return página de serviços publicados
     */
    @GetMapping
    public ResponseEntity<Page<RespostaServico>> listarPublicados(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String categorySlug,
            @PageableDefault(size = 20, sort = "ordemExibicao") Pageable pageable) {

        TipoServico tipo = parseTipoOuNull(type);
        Page<RespostaServico> pagina = catalogoService.listarServicosPublicados(tipo, categorySlug, pageable);
        return ResponseEntity.ok(pagina);
    }

    /**
     * {@code GET /api/v1/catalog/services/{slug}} — Detalhe de um serviço pelo slug.
     *
     * @param slug identificador amigável do serviço
     * @return dados completos do serviço publicado
     */
    @GetMapping("/{slug}")
    public ResponseEntity<RespostaServico> buscarPorSlug(@PathVariable String slug) {
        RespostaServico resposta = catalogoService.buscarServicoPublicadoPorSlug(slug);
        return ResponseEntity.ok(resposta);
    }

    /**
     * Converte a string do query param para o enum TipoServico, retornando
     * {@code null} se o parâmetro estiver ausente ou vazio.
     */
    private TipoServico parseTipoOuNull(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return TipoServico.valueOf(type.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
