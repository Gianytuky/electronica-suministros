package com.electronica.suministros.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Limpieza / eliminación de hongos solicitada para un detalle de la OT (recargo fijo de S/ 15.00). */
@Entity
@Table(name = "mantenimiento_cinta")
public class MantenimientoCinta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mantenimiento")
    private Integer idMantenimiento;

    @OneToOne
    @JoinColumn(name = "id_detalle")
    private DetalleOrden detalle;

    @Column(name = "aplica_limpieza")
    private Boolean aplicaLimpieza = Boolean.TRUE;

    @Column(name = "costo_adicional")
    private BigDecimal costoAdicional;

    // get y set
    public Integer getIdMantenimiento() { return idMantenimiento; }
    public void setIdMantenimiento(Integer idMantenimiento) { this.idMantenimiento = idMantenimiento; }
    public DetalleOrden getDetalle() { return detalle; }
    public void setDetalle(DetalleOrden detalle) { this.detalle = detalle; }
    public Boolean getAplicaLimpieza() { return aplicaLimpieza; }
    public void setAplicaLimpieza(Boolean aplicaLimpieza) { this.aplicaLimpieza = aplicaLimpieza; }
    public BigDecimal getCostoAdicional() { return costoAdicional; }
    public void setCostoAdicional(BigDecimal costoAdicional) { this.costoAdicional = costoAdicional; }
}
