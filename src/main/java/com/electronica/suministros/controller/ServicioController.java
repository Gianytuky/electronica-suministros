package com.electronica.suministros.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.electronica.suministros.dto.ServicioFormDTO;
import com.electronica.suministros.entity.ServicioTarifario;
import com.electronica.suministros.service.CategoriaService;
import com.electronica.suministros.service.NegocioException;
import com.electronica.suministros.service.ServicioService;

/** Mantenimiento del tarifario oficial de servicios (solo ADMIN). Alta por SP_Registrar_Servicio. */
@Controller
@RequestMapping("/admin/servicios")
public class ServicioController {

    private final ServicioService servicioService;
    private final CategoriaService categoriaService;

    public ServicioController(ServicioService servicioService, CategoriaService categoriaService) {
        this.servicioService = servicioService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("servicios", servicioService.listar());
        model.addAttribute("categorias", categoriaService.listar());
        model.addAttribute("cobros", List.of(ServicioTarifario.COBRO_POR_HORA, ServicioTarifario.COBRO_POR_UNIDAD));
        return "intranet/admin/servicios";
    }

    @PostMapping("/guardar")
    public String guardar(@Validated @ModelAttribute ServicioFormDTO form, BindingResult resultado, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            flash.addFlashAttribute("error", resultado.getFieldError().getDefaultMessage());
            return "redirect:/admin/servicios";
        }
        try {
            servicioService.guardar(form);
            flash.addFlashAttribute("ok", form.getIdServicio() == null ? "Servicio registrado en el tarifario." : "Servicio actualizado correctamente.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/servicios";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("ok", servicioService.eliminar(id));
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/servicios";
    }
}
