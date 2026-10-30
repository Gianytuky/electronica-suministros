package com.electronica.suministros.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** Tipo de material a digitalizar: Video, Audio o Imagen. Una categoría tiene muchos servicios del tarifario. */
@Entity
@Table(name = "categoria_material")
public class CategoriaMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    @Column(name = "nombre_categoria")
    private String nombreCategoria;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "imagen")
    private String imagen;

    @Column(name = "estado")
    private String estado = "ACTIVO";

    @OneToMany(mappedBy = "categoria")
    private List<ServicioTarifario> servicios = new ArrayList<>();

    // get y set
    public Integer getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Integer idCategoria) { this.idCategoria = idCategoria; }
    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<ServicioTarifario> getServicios() { return servicios; }
    public void setServicios(List<ServicioTarifario> servicios) { this.servicios = servicios; }
}
