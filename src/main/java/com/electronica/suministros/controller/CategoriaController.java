package com.electronica.suministros.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.electronica.suministros.service.CategoriaService;
import com.electronica.suministros.service.NegocioException;

/** Mantenimiento de categorías de material (Video, Audio, Imagen). Solo ADMIN. */
@Controller
@RequestMapping("/admin/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", categoriaService.listar());
        return "intranet/admin/categorias";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Integer idCategoria,
                          @RequestParam String nombreCategoria,
                          @RequestParam(required = false) String descripcion,
                          @RequestParam(required = false) String imagen,
                          @RequestParam(defaultValue = "ACTIVO") String estado,
                          RedirectAttributes flash) {
        try {
            categoriaService.guardar(idCategoria, nombreCategoria, descripcion, imagen, estado);
            flash.addFlashAttribute("ok", idCategoria == null ? "Categoría registrada correctamente." : "Categoría actualizada correctamente.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            categoriaService.eliminar(id);
            flash.addFlashAttribute("ok", "Categoría eliminada correctamente.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }
}
