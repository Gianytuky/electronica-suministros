package com.electronica.suministros.config;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Avance final (Spring Security): codificador de contraseñas.
 *
 * Las contraseñas nuevas se guardan con BCrypt ("{bcrypt}$2a$10$..."): lento a propósito y con sal aleatoria,
 * por lo que dos usuarios con la misma clave tienen hashes distintos.
 *
 * Compatibilidad: las bases cargadas en los avances anteriores guardan SHA-256 en hexadecimal, sin prefijo.
 * Esos valores se siguen aceptando al iniciar sesión y, en ese mismo inicio de sesión, Spring Security los
 * reemplaza automáticamente por BCrypt (UsuarioDetailsService.updatePassword). No hace falta migrar la base a mano.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        Sha256Heredado sha256 = new Sha256Heredado();
        Map<String, PasswordEncoder> codificadores = new HashMap<>();
        codificadores.put("bcrypt", new BCryptPasswordEncoder());
        codificadores.put("sha256", sha256);
        DelegatingPasswordEncoder codificador = new DelegatingPasswordEncoder("bcrypt", codificadores);
        // Un hash sin prefijo {id} es el SHA-256 de los avances anteriores
        codificador.setDefaultPasswordEncoderForMatches(sha256);
        return codificador;
    }

    /** SHA-256 en hexadecimal minúscula (mismo resultado que SHA2('clave', 256) de MySQL). Solo para leer hashes antiguos. */
    static final class Sha256Heredado implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] bytes = md.digest(rawPassword.toString().trim().getBytes(StandardCharsets.UTF_8));
                return String.format("%064x", new BigInteger(1, bytes));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 no disponible", e);
            }
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return rawPassword != null && encodedPassword != null
                    && encodedPassword.equalsIgnoreCase(encode(rawPassword));
        }

        @Override
        public boolean upgradeEncoding(String encodedPassword) {
            return true;
        }
    }
}
