package com.electronica.suministros.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.electronica.suministros.dto.UsuarioSesionDTO;
import com.electronica.suministros.entity.Rol;
import com.electronica.suministros.entity.Usuario;
import com.electronica.suministros.repository.CapturaTecnicaRepository;
import com.electronica.suministros.repository.OrdenTrabajoRepository;
import com.electronica.suministros.repository.RolRepository;
import com.electronica.suministros.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final OrdenTrabajoRepository ordenRepo;
    private final CapturaTecnicaRepository capturaRepo;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepo, RolRepository rolRepo,
                          OrdenTrabajoRepository ordenRepo, CapturaTecnicaRepository capturaRepo,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.ordenRepo = ordenRepo;
        this.capturaRepo = capturaRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Avance final (Spring Security): la verificación del correo y la contraseña la hace Spring Security
     * (UsuarioDetailsService). Este método solo arma los datos que se guardan en la sesión tras un login correcto.
     */
    @Transactional(readOnly = true)
    public UsuarioSesionDTO datosSesion(String correo) {
        Usuario u = usuarioRepo.findByCorreoIgnoreCase(correo == null ? "" : correo.trim())
                .orElseThrow(() -> new NegocioException("El usuario no existe"));
        return new UsuarioSesionDTO(u.getIdUsuario(), u.getNombres(), u.getCorreo(), u.getRol().getNombreRol());
    }

    /** Avance final: contraseña cifrada con BCrypt (antes SHA-256). La usa la administración de usuarios (RecuperacionService usa el mismo PasswordEncoder). */
    public String cifrar(String password) {
        return passwordEncoder.encode(password.trim());
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepo.findAllByOrderByNombresAsc();
    }

    @Transactional(readOnly = true)
    public List<Rol> listarRoles() {
        return rolRepo.findAll();
    }

    /** Crea o actualiza un usuario. En edición, una contraseña vacía conserva la anterior. */
    @Transactional
    public void guardar(Integer idUsuario, String nombres, String correo, String password,
                        Integer idRol, String estado, Integer idUsuarioActual) {

        if (nombres == null || nombres.isBlank()) {
            throw new NegocioException("Ingrese los nombres del usuario");
        }
        if (correo == null || !correo.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new NegocioException("Ingrese un correo válido");
        }
        if (idRol == null) {
            throw new NegocioException("Seleccione un rol");
        }
        Rol rol = rolRepo.findById(idRol).orElseThrow(() -> new NegocioException("El rol no existe"));
        String estadoFinal = "INACTIVO".equals(estado) ? "INACTIVO" : "ACTIVO";
        String correoFinal = correo.trim().toLowerCase();

        if (idUsuario == null) {
            if (password == null || password.trim().length() < 3) {
                throw new NegocioException("La contraseña debe tener al menos 3 caracteres");
            }
            if (usuarioRepo.existsByCorreoIgnoreCase(correoFinal)) {
                throw new NegocioException("Ya existe un usuario con el correo " + correoFinal);
            }
            Usuario nuevo = new Usuario();
            nuevo.setNombres(nombres.trim());
            nuevo.setCorreo(correoFinal);
            nuevo.setPassword(cifrar(password));
            nuevo.setRol(rol);
            nuevo.setEstado(estadoFinal);
            usuarioRepo.save(nuevo);
            return;
        }

        Usuario u = usuarioRepo.findById(idUsuario).orElseThrow(() -> new NegocioException("El usuario no existe"));
        if (usuarioRepo.existsByCorreoIgnoreCaseAndIdUsuarioNot(correoFinal, idUsuario)) {
            throw new NegocioException("Ya existe otro usuario con el correo " + correoFinal);
        }
        if (idUsuario.equals(idUsuarioActual) && ("INACTIVO".equals(estadoFinal) || !"ADMIN".equals(rol.getNombreRol()))) {
            throw new NegocioException("No puede desactivarse ni quitarse el rol ADMIN a usted mismo");
        }
        u.setNombres(nombres.trim());
        u.setCorreo(correoFinal);
        u.setRol(rol);
        u.setEstado(estadoFinal);
        if (password != null && !password.isBlank()) {
            if (password.trim().length() < 3) {
                throw new NegocioException("La contraseña debe tener al menos 3 caracteres");
            }
            u.setPassword(cifrar(password));
        }
        usuarioRepo.save(u);
    }

    /** Elimina al usuario; si ya registró OTs o capturas solo se desactiva (baja lógica). Devuelve el mensaje a mostrar. */
    @Transactional
    public String eliminar(Integer idUsuario, Integer idUsuarioActual) {
        if (idUsuario.equals(idUsuarioActual)) {
            throw new NegocioException("No puede eliminar su propia cuenta");
        }
        Usuario u = usuarioRepo.findById(idUsuario).orElseThrow(() -> new NegocioException("El usuario no existe"));
        if (ordenRepo.countByEjecutivo_IdUsuario(idUsuario) > 0 || capturaRepo.countByTecnico_IdUsuario(idUsuario) > 0) {
            u.setEstado("INACTIVO");
            usuarioRepo.save(u);
            return "El usuario tiene OTs o capturas registradas, por eso se desactivó en lugar de eliminarse.";
        }
        usuarioRepo.delete(u);
        return "Usuario eliminado correctamente.";
    }
}
