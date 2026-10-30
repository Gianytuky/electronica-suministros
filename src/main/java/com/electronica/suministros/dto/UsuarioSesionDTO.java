package com.electronica.suministros.dto;

import java.io.Serializable;

/**
 * Datos mínimos del usuario autenticado que se guardan en la HttpSession.
 * Nunca se guarda la entidad Usuario (ni su contraseña) en la sesión.
 */
public class UsuarioSesionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer idUsuario;
    private String nombres;
    private String correo;
    private String rol; // ADMIN | EJECUTIVO | TECNICO

    public UsuarioSesionDTO() {
    }

    public UsuarioSesionDTO(Integer idUsuario, String nombres, String correo, String rol) {
        this.idUsuario = idUsuario;
        this.nombres = nombres;
        this.correo = correo;
        this.rol = rol;
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
