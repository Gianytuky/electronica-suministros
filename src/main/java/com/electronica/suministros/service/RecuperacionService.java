package com.electronica.suministros.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.electronica.suministros.entity.Usuario;
import com.electronica.suministros.repository.UsuarioRepository;

/**
 * Recuperación de contraseña con token temporal (UUID). Adaptación de la lógica de la práctica S13
 * a la arquitectura del proyecto. Desde la incorporación de Spring Security la nueva clave se guarda con
 * BCrypt (el mismo PasswordEncoder que valida el inicio de sesión); /recover y /reset son rutas públicas.
 *
 * Flujo:
 *   1. solicitar(correo): genera un UUID, lo guarda en usuario.reset_token con su vencimiento y envía
 *      el enlace /reset?token=... por correo. NUNCA revela si el correo existe.
 *   2. tokenValido(token): comprueba que el token exista, no haya vencido y el usuario siga ACTIVO.
 *   3. restablecer(token, clave, confirmación): guarda la nueva clave (BCrypt) y borra el token,
 *      de modo que el enlace solo sirve una vez.
 */
@Service
public class RecuperacionService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionService.class);

    /** Misma longitud mínima que usa la administración de usuarios. */
    public static final int MIN_PASSWORD = 3;

    private static final Pattern FORMATO_UUID =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** Tiempo mínimo entre dos solicitudes del mismo usuario (evita llenar su bandeja de correos). */
    private static final int SEGUNDOS_ENTRE_SOLICITUDES = 60;

    private final UsuarioRepository usuarioRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final String baseUrl;
    private final int minutosVigencia;

    public RecuperacionService(UsuarioRepository usuarioRepo, EmailService emailService,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.base-url:http://localhost:8083}") String baseUrl,
                               @Value("${app.recuperacion.token-minutos:15}") int minutosVigencia) {
        this.usuarioRepo = usuarioRepo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.minutosVigencia = minutosVigencia;
    }

    /**
     * Paso 1. No devuelve nada a propósito: el controlador siempre muestra el mismo mensaje, exista o no
     * el correo, para que nadie pueda averiguar qué correos están registrados.
     * No es @Transactional: el token se guarda primero y el correo se envía después, fuera de la transacción.
     */
    public void solicitar(String correo) {
        if (correo == null || correo.isBlank()) {
            return;
        }
        Usuario u = usuarioRepo.findByCorreoIgnoreCase(correo.trim()).orElse(null);
        if (u == null || !"ACTIVO".equals(u.getEstado())) {
            log.info("Recuperación solicitada para un correo no registrado o inactivo: no se envía nada.");
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (u.getResetTokenExpira() != null
                && u.getResetTokenExpira().minusMinutes(minutosVigencia)
                        .plusSeconds(SEGUNDOS_ENTRE_SOLICITUDES).isAfter(ahora)) {
            log.info("Ya se envió un enlace de recuperación hace menos de {} s a {}: se ignora la nueva solicitud.",
                    SEGUNDOS_ENTRE_SOLICITUDES, u.getCorreo());
            return;
        }

        String token = UUID.randomUUID().toString();
        u.setResetToken(token);
        u.setResetTokenExpira(ahora.plusMinutes(minutosVigencia));
        usuarioRepo.save(u);

        String enlace = baseUrl + "/reset?token=" + token;
        try {
            emailService.tokenRecuperacion(u.getCorreo(), u.getNombres(), enlace, minutosVigencia);
        } catch (CorreoException e) {
            // Sin correo no hay forma de usar el token: se anula para poder reintentar de inmediato.
            log.warn("No se pudo enviar el enlace de recuperación a {}: {}", u.getCorreo(), e.getMessage());
            u.setResetToken(null);
            u.setResetTokenExpira(null);
            usuarioRepo.save(u);
        }
    }

    /** Paso 2: ¿el enlace aún sirve? */
    @Transactional(readOnly = true)
    public boolean tokenValido(String token) {
        return buscarVigente(token).isPresent();
    }

    /** Paso 3: guarda la nueva contraseña (BCrypt) y consume el token. */
    @Transactional
    public void restablecer(String token, String password, String confirmacion) {
        Usuario u = buscarVigente(token).orElseThrow(() ->
                new NegocioException("El enlace de recuperación no es válido o ya venció. Solicite uno nuevo."));
        if (password == null || password.trim().length() < MIN_PASSWORD) {
            throw new NegocioException("La contraseña debe tener al menos " + MIN_PASSWORD + " caracteres.");
        }
        if (confirmacion == null || !password.trim().equals(confirmacion.trim())) {
            throw new NegocioException("Las contraseñas no coinciden.");
        }
        u.setPassword(passwordEncoder.encode(password.trim()));
        u.setResetToken(null);
        u.setResetTokenExpira(null);
        usuarioRepo.save(u);
        log.info("Contraseña restablecida con token para el usuario {}", u.getCorreo());
    }

    private Optional<Usuario> buscarVigente(String token) {
        if (token == null || !FORMATO_UUID.matcher(token.trim()).matches()) {
            return Optional.empty();
        }
        return usuarioRepo.findByResetToken(token.trim().toLowerCase())
                .filter(u -> "ACTIVO".equals(u.getEstado()))
                .filter(u -> u.getResetTokenExpira() != null && u.getResetTokenExpira().isAfter(LocalDateTime.now()));
    }
}
