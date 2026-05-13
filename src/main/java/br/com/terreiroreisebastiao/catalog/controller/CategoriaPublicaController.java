package br.com.terreiroreisebastiao.catalog.controller;

import br.com.terreiroreisebastiao.catalog.dto.RespostaCategoria;
import br.com.terreiroreisebastiao.catalog.service.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller REST público para listagem de categorias do catálogo.
 *
 * <p>Acessível sem autenticação. Retorna apenas categorias publicadas.</p>
 *
 * @see CatalogoService
 */
@RestController
@RequestMapping("/api/v1/catalog/categories")
public class CategoriaPublicaController {

    private final CatalogoService catalogoService;

    public CategoriaPublicaController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /**
     * {@code GET /api/v1/catalog/categories} — Lista categorias publicadas.
     *
     * @return lista de categorias visíveis ao público
     */
    @GetMapping
    public ResponseEntity<List<RespostaCategoria>> listarPublicadas() {
        List<RespostaCategoria> categorias = catalogoService.listarCategoriasPublicadas();
        return ResponseEntity.ok(categorias);
    }
}
