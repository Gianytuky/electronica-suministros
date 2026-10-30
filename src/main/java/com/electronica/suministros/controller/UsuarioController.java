package com.electronica.suministros.controller;

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
import com.electronica.suministros.service.NegocioException;
import com.electronica.suministros.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

/** Mantenimiento de usuarios (solo ADMIN). Alta y edición se hacen en una ventana modal. */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("roles", usuarioService.listarRoles());
        return "intranet/admin/usuarios";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Integer idUsuario,
                          @RequestParam String nombres,
                          @RequestParam String correo,
                          @RequestParam(required = false) String password,
                          @RequestParam Integer idRol,
                          @RequestParam(defaultValue = "ACTIVO") String estado,
                          HttpSession session,
                          RedirectAttributes flash) {
        UsuarioSesionDTO actual = (UsuarioSesionDTO) session.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        try {
            usuarioService.guardar(idUsuario, nombres, correo, password, idRol, estado, actual.getIdUsuario());
            flash.addFlashAttribute("ok", idUsuario == null ? "Usuario registrado correctamente." : "Usuario actualizado correctamente.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, HttpSession session, RedirectAttributes flash) {
        UsuarioSesionDTO actual = (UsuarioSesionDTO) session.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        try {
            flash.addFlashAttribute("ok", usuarioService.eliminar(id, actual.getIdUsuario()));
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}
