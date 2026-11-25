package com.electronica.suministros.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.electronica.suministros.config.AuthInterceptor;
import com.electronica.suministros.dto.UsuarioSesionDTO;
import com.electronica.suministros.service.NegocioException;
import com.electronica.suministros.service.RecuperacionService;

import jakarta.servlet.http.HttpSession;

/**
 * Pantalla de inicio de sesión (templates/auth/login.html) y recuperación de contraseña con token UUID
 * (recover.html y reset.html).
 *
 * Avance final (Spring Security): el POST /login (verificación del correo y la contraseña BCrypt) y el
 * POST /logout los procesa Spring Security (SecurityConfig); al iniciar sesión, LoginExitosoHandler guarda
 * el usuario en la sesión y redirige con rutaSegunRol(). Este controlador solo muestra las vistas.
 * /login, /recover y /reset son rutas públicas.
 */
@Controller
public class AuthController {

    private final RecuperacionService recuperacionService;

    public AuthController(RecuperacionService recuperacionService) {
        this.recuperacionService = recuperacionService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String denegado,
                        @RequestParam(required = false) String reset,
                        HttpSession session,
                        Model model) {

        UsuarioSesionDTO enSesion = (UsuarioSesionDTO) session.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        if (enSesion != null) {
            return "redirect:" + rutaSegunRol(enSesion.getRol());
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
        if (reset != null) {
            model.addAttribute("resetMsg", "Su contraseña fue actualizada. Ya puede iniciar sesión con la nueva.");
        }
        return "auth/login";
    }

    // ------------------------------------------------------------------ recuperación de contraseña

    /** Formulario "¿Olvidaste tu contraseña?": pide el correo de la cuenta. */
    @GetMapping("/recover")
    public String recover(@RequestParam(required = false) String enviado, Model model) {
        if (enviado != null) {
            model.addAttribute("enviadoMsg", "Si el correo corresponde a una cuenta activa, le enviamos un enlace "
                    + "para crear una nueva contraseña. Revise su bandeja de entrada y la carpeta de spam.");
        }
        return "auth/recover";
    }

    /** Siempre responde igual (exista o no el correo) para no revelar qué cuentas están registradas. */
    @PostMapping("/recover")
    public String procesarRecover(@RequestParam String correo) {
        recuperacionService.solicitar(correo);
        return "redirect:/recover?enviado=true";
    }

    /** Enlace del correo: muestra el formulario de nueva contraseña si el token aún es válido. */
    @GetMapping("/reset")
    public String reset(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("tokenValido", recuperacionService.tokenValido(token));
        return "auth/reset";
    }

    @PostMapping("/reset")
    public String procesarReset(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmacion,
                                Model model) {
        try {
            recuperacionService.restablecer(token, password, confirmacion);
            return "redirect:/login?reset=true";
        } catch (NegocioException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("token", token);
            model.addAttribute("tokenValido", recuperacionService.tokenValido(token));
            return "auth/reset";
        }
    }

    /** Módulo de inicio de cada rol (lo usan GET /login y LoginExitosoHandler). */
    public static String rutaSegunRol(String rol) {
        return switch (rol == null ? "" : rol) {
            case "ADMIN" -> "/admin/dashboard";
            case "EJECUTIVO" -> "/ejecutivo/ordenes";
            case "TECNICO" -> "/tecnico/panel";
            default -> "/";
        };
    }
}
