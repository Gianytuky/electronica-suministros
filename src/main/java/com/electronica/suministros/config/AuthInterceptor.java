package com.electronica.suministros.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.electronica.suministros.dto.UsuarioSesionDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Control de Acceso Basado en Roles (RBAC) por carpeta de URL:
 *
 *   /ejecutivo/**, /api/ordenes/**  -> EJECUTIVO y ADMIN   (recepción, OTs, clientes, cobros)
 *   /tecnico/**                     -> TECNICO y ADMIN      (laboratorio: captura técnica)
 *   /admin/**, /api/reportes/**     -> solo ADMIN           (dashboard, tarifario, usuarios)
 *
 * El ADMIN tiene acceso total (regla heredada del Avance 2).
 * Sin sesión se redirige al login; con sesión pero sin permiso se responde 403,
 * que Spring Boot muestra con templates/error/403.html.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_SESION = "usuario";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String uri = request.getRequestURI();
        boolean esApi = uri.startsWith("/api/");

        HttpSession session = request.getSession(false);
        UsuarioSesionDTO usuario = (session == null)
                ? null
                : (UsuarioSesionDTO) session.getAttribute(ATRIBUTO_SESION);

        // Evita que el botón "Atrás" muestre pantallas privadas después de cerrar sesión
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        if (usuario == null) {
            if (esApi) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.sendRedirect(request.getContextPath() + "/login?denegado=true");
            }
            return false;
        }

        if (!tienePermiso(uri, usuario.getRol())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    private boolean tienePermiso(String uri, String rol) {
        if ("ADMIN".equals(rol)) {
            return true;
        }
        if (uri.startsWith("/ejecutivo") || uri.startsWith("/api/ordenes")) {
            return "EJECUTIVO".equals(rol);
        }
        if (uri.startsWith("/tecnico")) {
            return "TECNICO".equals(rol);
        }
        return false; // /admin/** y /api/reportes/** son exclusivas del ADMIN
    }
}
