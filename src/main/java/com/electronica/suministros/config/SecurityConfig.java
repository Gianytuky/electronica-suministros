package com.electronica.suministros.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Avance final: Spring Security.
 *
 * Mismas reglas de acceso por rol (RBAC) que el sistema ya tenía:
 *   /admin/**, /api/reportes/**                       -> solo ADMIN
 *   /ejecutivo/**, /api/ordenes/**, /api/reniec/**    -> EJECUTIVO y ADMIN
 *   /tecnico/**                                       -> TECNICO y ADMIN
 *   todo lo demás (web pública, /login, /recover, /reset, /api/catalogo/**, recursos estáticos) -> público
 *
 * Qué aporta Spring Security:
 *  - Autenticación con UsuarioDetailsService y contraseñas BCrypt (PasswordConfig).
 *  - Protección CSRF en todos los formularios POST (Thymeleaf agrega el token en cada th:action)
 *    y en el fetch de orden_form.html (cabecera X-CSRF-TOKEN leída de las etiquetas meta de fragments/header).
 *  - Cambio del id de sesión al iniciar sesión (contra la fijación de sesión) y cabeceras de seguridad
 *    (no-cache en las respuestas, X-Frame-Options, X-Content-Type-Options).
 *
 * El AuthInterceptor se conserva como segunda capa: repite la verificación del rol con el usuario de la sesión.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, LoginExitosoHandler loginExitoso) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**", "/api/reportes/**").hasRole("ADMIN")
                .requestMatchers("/ejecutivo/**", "/api/ordenes/**", "/api/reniec/**").hasAnyRole("EJECUTIVO", "ADMIN")
                .requestMatchers("/tecnico/**").hasAnyRole("TECNICO", "ADMIN")
                .anyRequest().permitAll())
            // Se reutiliza la pantalla propia templates/auth/login.html (campos "correo" y "password")
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("correo")
                .passwordParameter("password")
                .successHandler(loginExitoso)
                .failureUrl("/login?error=true")
                .permitAll())
            // El cierre de sesión es POST (con token CSRF): los botones "Cerrar sesión" son formularios
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            // Sin sesión: las APIs responden 401 y las pantallas vuelven al login, igual que antes
            .exceptionHandling(ex -> ex.authenticationEntryPoint(SecurityConfig::sinSesion));
        return http.build();
    }

    private static void sinSesion(HttpServletRequest request, HttpServletResponse response,
                                  AuthenticationException e) throws IOException {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.sendRedirect(request.getContextPath() + "/login?denegado=true");
        }
    }
}
