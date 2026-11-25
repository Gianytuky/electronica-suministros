package com.electronica.suministros.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Persona que inicia sesión en la intranet. Muchos usuarios pertenecen a un rol. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @ManyToOne
    @JoinColumn(name = "id_rol")
    private Rol rol;

    @Column(name = "nombres")
    private String nombres;

    @Column(name = "correo")
    private String correo;

    @Column(name = "password")
    private String password;

    @Column(name = "estado")
    private String estado;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    /** Avance final: token UUID de un solo uso para recuperar la contraseña (NULL si no hay recuperación en curso). */
    @Column(name = "reset_token")
    private String resetToken;

    /** Fecha y hora límite para usar el token. */
    @Column(name = "reset_token_expira")
    private LocalDateTime resetTokenExpira;

    /** OTs que este usuario registró como ejecutivo de mostrador (relación 1 a N). */
    @OneToMany(mappedBy = "ejecutivo")
    private List<OrdenTrabajo> ordenes = new ArrayList<>();

    // get y set
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    public LocalDateTime getResetTokenExpira() { return resetTokenExpira; }
    public void setResetTokenExpira(LocalDateTime resetTokenExpira) { this.resetTokenExpira = resetTokenExpira; }
    public List<OrdenTrabajo> getOrdenes() { return ordenes; }
    public void setOrdenes(List<OrdenTrabajo> ordenes) { this.ordenes = ordenes; }
}
