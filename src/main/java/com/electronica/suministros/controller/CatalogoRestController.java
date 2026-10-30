package com.electronica.suministros.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.electronica.suministros.dto.CategoriaCatalogoDTO;
import com.electronica.suministros.dto.ServicioCatalogoDTO;
import com.electronica.suministros.service.CategoriaService;
import com.electronica.suministros.service.ServicioService;

/**
 * API pública para los combos en cascada Categoría -> Formato (cotizador público y formulario de OT).
 * Devuelve records Java que Jackson convierte a JSON; no requiere sesión.
 */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoRestController {

    private final CategoriaService categoriaService;
    private final ServicioService servicioService;

    public CatalogoRestController(CategoriaService categoriaService, ServicioService servicioService) {
        this.categoriaService = categoriaService;
        this.servicioService = servicioService;
    }

    @GetMapping("/categorias")
    public List<CategoriaCatalogoDTO> categorias() {
        return categoriaService.catalogo();
    }

    @GetMapping("/servicios")
    public List<ServicioCatalogoDTO> servicios(@RequestParam Integer categoria) {
        return servicioService.porCategoria(categoria);
    }
}
