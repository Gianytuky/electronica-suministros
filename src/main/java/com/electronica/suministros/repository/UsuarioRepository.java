package com.electronica.suministros.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.electronica.suministros.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    /** Recuperación de contraseña: busca al usuario dueño de un token UUID. */
    Optional<Usuario> findByResetToken(String resetToken);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdUsuarioNot(String correo, Integer idUsuario);

    List<Usuario> findAllByOrderByNombresAsc();
}
