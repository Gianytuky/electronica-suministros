package com.electronica.suministros.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Servicio del tarifario oficial: un formato (VHS, LP, Fotos...) con su precio base y su forma de cobro. */
@Entity
@Table(name = "servicio_tarifario")
public class ServicioTarifario {

    public static final String COBRO_POR_HORA = "Por Hora o Fracción";
    public static final String COBRO_POR_UNIDAD = "Por Unidad";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Integer idServicio;

    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private CategoriaMaterial categoria;

    @Column(name = "codigo")
    private String codigo;

    @Column(name = "formato")
    private String formato;

    @Column(name = "tipo_cobro")
    private String tipoCobro;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "imagen")
    private String imagen;

    @Column(name = "estado")
    private String estado = "ACTIVO";

    /** Los formatos de Video se cobran por la duración real capturada (horas). */
    public boolean isEsVideo() {
        return categoria != null && "Video".equalsIgnoreCase(categoria.getNombreCategoria());
    }

    /** La limpieza / mantenimiento de cinta no aplica a fotografías, slides ni negativos. */
    public boolean isAdmiteLimpieza() {
        return categoria != null && !"Imagen".equalsIgnoreCase(categoria.getNombreCategoria());
    }

    // get y set
    public Integer getIdServicio() { return idServicio; }
    public void setIdServicio(Integer idServicio) { this.idServicio = idServicio; }
    public CategoriaMaterial getCategoria() { return categoria; }
    public void setCategoria(CategoriaMaterial categoria) { this.categoria = categoria; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }
    public String getTipoCobro() { return tipoCobro; }
    public void setTipoCobro(String tipoCobro) { this.tipoCobro = tipoCobro; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
