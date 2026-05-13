package br.com.terreiroreisebastiao.catalog.controller;

import br.com.terreiroreisebastiao.catalog.domain.TipoServico;
import br.com.terreiroreisebastiao.catalog.dto.RequisicaoCriarServico;
import br.com.terreiroreisebastiao.catalog.dto.RespostaServico;
import br.com.terreiroreisebastiao.catalog.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

/**
 * Controller REST blindado para gestão administrativa de serviços.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/admin/servicos", "/api/v1/admin/catalog/services"})
public class ServicoController {

    private final CatalogoService catalogoService;

    @GetMapping
    public ResponseEntity<Page<RespostaServico>> listarTodos(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 20, sort = "ordemExibicao") Pageable pageable) {
        TipoServico tipo = parseTipoOuNull(type);
        return ResponseEntity.ok(catalogoService.listarTodosServicos(tipo, categoryId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespostaServico> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogoService.buscarServico(id));
    }

    @PostMapping
    public ResponseEntity<RespostaServico> criar(
            @Valid @RequestBody RequisicaoCriarServico requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogoService.criarServico(requisicao));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespostaServico> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody RequisicaoCriarServico requisicao) {
        return ResponseEntity.ok(catalogoService.substituirServico(id, requisicao));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<RespostaServico> publicar(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogoService.publicarServico(id));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<RespostaServico> despublicar(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogoService.despublicarServico(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        catalogoService.removerServico(id);
        return ResponseEntity.noContent().build();
    }

    private TipoServico parseTipoOuNull(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return TipoServico.valueOf(type.trim().toUpperCase(Locale.ROOT));
    }
}