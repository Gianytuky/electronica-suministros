package com.electronica.suministros.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.electronica.suministros.config.AuthInterceptor;
import com.electronica.suministros.dto.UsuarioSesionDTO;
import com.electronica.suministros.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Inicio y cierre de sesión (vista templates/auth/login.html). */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String denegado,
                        HttpSession session,
                        Model model) {

        UsuarioSesionDTO enSesion = (UsuarioSesionDTO) session.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        if (enSesion != null) {
            return redirigirSegunRol(enSesion.getRol());
        }
        if (error != null) {
            model.addAttribute("errorMsg", "Credenciales incorrectas o usuario inactivo. Verifique su correo y contraseña.");
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "Ha cerrado su sesión correctamente.");
        }
        if (denegado != null) {
            model.addAttribute("denegadoMsg", "Inicie sesión para acceder a ese módulo.");
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
                                @RequestParam String password,
                                HttpServletRequest request) {

        UsuarioSesionDTO usuario = usuarioService.autenticar(correo, password);
        if (usuario == null) {
            return "redirect:/login?error=true";
        }
        // Si ya existía una sesión anónima, se cambia su id (protege contra fijación de sesión)
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        request.getSession(true).setAttribute(AuthInterceptor.ATRIBUTO_SESION, usuario);
        return redirigirSegunRol(usuario.getRol());
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    private String redirigirSegunRol(String rol) {
        return switch (rol) {
            case "ADMIN" -> "redirect:/admin/dashboard";
            case "EJECUTIVO" -> "redirect:/ejecutivo/ordenes";
            case "TECNICO" -> "redirect:/tecnico/panel";
            default -> "redirect:/";
        };
    }
}
