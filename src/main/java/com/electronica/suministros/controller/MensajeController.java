package com.electronica.suministros.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.electronica.suministros.config.AuthInterceptor;
import com.electronica.suministros.dto.UsuarioSesionDTO;
import com.electronica.suministros.service.MensajeContactoService;
import com.electronica.suministros.service.NegocioException;

import jakarta.servlet.http.HttpSession;

/** Bandeja de consultas recibidas desde el formulario público (EJECUTIVO y ADMIN). */
@Controller
@RequestMapping("/ejecutivo/mensajes")
public class MensajeController {

    private final MensajeContactoService mensajeService;

    public MensajeController(MensajeContactoService mensajeService) {
        this.mensajeService = mensajeService;
    }

    @GetMapping
    public String bandeja(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                          Model model) {
        model.addAttribute("mensajes", mensajeService.listar(desde, hasta));
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "intranet/ejecutivo/mensajes";
    }

    @PostMapping("/leido/{id}")
    public String marcarLeido(@PathVariable Integer id, RedirectAttributes flash) {
        mensajeService.marcarLeido(id);
        flash.addFlashAttribute("ok", "Mensaje marcado como leído.");
        return "redirect:/ejecutivo/mensajes";
    }

    /** Avance final: responde la consulta por correo (modal "Responder" de mensajes.html). */
    @PostMapping("/responder")
    public String responder(@RequestParam Integer idMensaje,
                            @RequestParam String asunto,
                            @RequestParam String respuesta,
                            HttpSession session,
                            RedirectAttributes flash) {
        UsuarioSesionDTO actual = (UsuarioSesionDTO) session.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        try {
            String destino = mensajeService.responder(idMensaje, asunto, respuesta, actual.getNombres());
            flash.addFlashAttribute("ok", "Respuesta enviada por correo a " + destino + ". La consulta quedó marcada como leída.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ejecutivo/mensajes";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        mensajeService.eliminar(id);
        flash.addFlashAttribute("ok", "Mensaje eliminado.");
        return "redirect:/ejecutivo/mensajes";
    }
}
