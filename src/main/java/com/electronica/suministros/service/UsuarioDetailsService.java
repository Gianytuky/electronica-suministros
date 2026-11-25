package com.electronica.suministros.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.electronica.suministros.entity.Usuario;
import com.electronica.suministros.repository.UsuarioRepository;

/**
 * Avance final (Spring Security): carga desde MySQL el usuario que intenta iniciar sesión.
 *  - El "username" es el correo (sin distinguir mayúsculas).
 *  - El rol de la tabla rol (ADMIN, EJECUTIVO, TECNICO) se convierte en la autoridad ROLE_xxx.
 *  - Un usuario INACTIVO queda deshabilitado: Spring Security rechaza el inicio de sesión.
 *  - updatePassword: si la contraseña guardada aún es SHA-256 (avances anteriores), Spring Security la
 *    reemplaza por BCrypt en el primer inicio de sesión correcto.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioDetailsService.class);

    private final UsuarioRepository usuarioRepo;

    public UsuarioDetailsService(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        String buscado = correo == null ? "" : correo.trim();
        Usuario u = usuarioRepo.findByCorreoIgnoreCase(buscado)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
        return User.withUsername(u.getCorreo())
                .password(u.getPassword())
                .roles(u.getRol().getNombreRol())
                .disabled(!"ACTIVO".equals(u.getEstado()))
                .build();
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        usuarioRepo.findByCorreoIgnoreCase(user.getUsername()).ifPresent(u -> {
            u.setPassword(newPassword);
            usuarioRepo.save(u);
            log.info("Contraseña de {} actualizada de SHA-256 a BCrypt.", u.getCorreo());
        });
        return User.withUserDetails(user).password(newPassword).build();
    }
}
