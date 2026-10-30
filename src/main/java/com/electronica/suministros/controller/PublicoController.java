package com.electronica.suministros.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.electronica.suministros.service.CategoriaService;
import com.electronica.suministros.service.MensajeContactoService;
import com.electronica.suministros.service.NegocioException;
import com.electronica.suministros.service.ServicioService;

/** Páginas públicas (sin sesión): inicio, catálogo de servicios con cotizador y contacto. */
@Controller
public class PublicoController {

    private final CategoriaService categoriaService;
    private final ServicioService servicioService;
    private final MensajeContactoService mensajeService;

    public PublicoController(CategoriaService categoriaService, ServicioService servicioService,
                             MensajeContactoService mensajeService) {
        this.categoriaService = categoriaService;
        this.servicioService = servicioService;
        this.mensajeService = mensajeService;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("categorias", categoriaService.listarActivas());
        return "publico/inicio";
    }

    /** Catálogo visual + cotizador. El cotizador usa los combos en cascada (Categoría -> Formato) vía /api/catalogo. */
    @GetMapping("/servicios")
    public String servicios(Model model) {
        model.addAttribute("categorias", categoriaService.listarActivas());
        model.addAttribute("servicios", servicioService.listarActivos());
        return "publico/servicios";
    }

    @GetMapping("/contacto")
    public String contacto() {
        return "publico/contacto";
    }

    @PostMapping("/contacto")
    public String enviarContacto(@RequestParam String nombre,
                                 @RequestParam String correo,
                                 @RequestParam(required = false) String telefono,
                                 @RequestParam String mensaje,
                                 RedirectAttributes flash) {
        try {
            mensajeService.registrar(nombre, correo, telefono, mensaje);
            flash.addFlashAttribute("ok", "¡Gracias por escribirnos! Su mensaje fue enviado y le responderemos pronto.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/contacto";
    }
}
