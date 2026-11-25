package com.electronica.suministros.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.electronica.suministros.controller.AuthController;
import com.electronica.suministros.dto.UsuarioSesionDTO;
import com.electronica.suministros.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Avance final (Spring Security): se ejecuta después de un inicio de sesión correcto.
 * Guarda en la sesión el mismo UsuarioSesionDTO de siempre (atributo "usuario"), que usan los controladores,
 * las plantillas (session.usuario) y el AuthInterceptor, y redirige al módulo del rol.
 */
@Component
public class LoginExitosoHandler implements AuthenticationSuccessHandler {

    private final UsuarioService usuarioService;

    public LoginExitosoHandler(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        UsuarioSesionDTO usuario = usuarioService.datosSesion(authentication.getName());
        request.getSession(true).setAttribute(AuthInterceptor.ATRIBUTO_SESION, usuario);
        response.sendRedirect(request.getContextPath() + AuthController.rutaSegunRol(usuario.getRol()));
    }
}
