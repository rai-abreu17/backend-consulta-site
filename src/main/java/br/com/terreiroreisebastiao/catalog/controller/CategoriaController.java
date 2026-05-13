package br.com.terreiroreisebastiao.catalog.controller;

import br.com.terreiroreisebastiao.catalog.dto.RequisicaoCategoria;
import br.com.terreiroreisebastiao.catalog.dto.RespostaCategoria;
import br.com.terreiroreisebastiao.catalog.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST blindado para gestão administrativa de categorias.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/admin/categorias", "/api/v1/admin/catalog/categories"})
public class CategoriaController {

    private final CatalogoService catalogoService;

    @GetMapping
    public ResponseEntity<List<RespostaCategoria>> listarTodas() {
        return ResponseEntity.ok(catalogoService.listarTodasCategorias());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespostaCategoria> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogoService.buscarCategoria(id));
    }

    @PostMapping
    public ResponseEntity<RespostaCategoria> criar(
            @Valid @RequestBody RequisicaoCategoria requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogoService.criarCategoria(requisicao));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespostaCategoria> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody RequisicaoCategoria requisicao) {
        return ResponseEntity.ok(catalogoService.atualizarCategoria(id, requisicao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        catalogoService.removerCategoria(id);
        return ResponseEntity.noContent().build();
    }
}