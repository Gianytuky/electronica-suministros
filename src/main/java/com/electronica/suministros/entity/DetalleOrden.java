package com.electronica.suministros.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Línea de la OT: formato a digitalizar, cantidad (unidades u horas estimadas) y tarifa vigente.
 * Puede tener un mantenimiento (limpieza de cinta) y, cuando el laboratorio la procesa, una captura técnica.
 */
@Entity
@Table(name = "detalle_orden")
public class DetalleOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne
    @JoinColumn(name = "id_ot")
    private OrdenTrabajo ot;

    @ManyToOne
    @JoinColumn(name = "id_servicio")
    private ServicioTarifario servicio;

    @Column(name = "cantidad_materiales")
    private Integer cantidadMateriales;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @OneToOne(mappedBy = "detalle", cascade = CascadeType.ALL, orphanRemoval = true)
    private MantenimientoCinta mantenimiento;

    @OneToOne(mappedBy = "detalle", cascade = CascadeType.ALL, orphanRemoval = true)
    private CapturaTecnica captura;

    public boolean isConLimpieza() {
        return mantenimiento != null && Boolean.TRUE.equals(mantenimiento.getAplicaLimpieza());
    }

    // get y set
    public Integer getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Integer idDetalle) { this.idDetalle = idDetalle; }
    public OrdenTrabajo getOt() { return ot; }
    public void setOt(OrdenTrabajo ot) { this.ot = ot; }
    public ServicioTarifario getServicio() { return servicio; }
    public void setServicio(ServicioTarifario servicio) { this.servicio = servicio; }
    public Integer getCantidadMateriales() { return cantidadMateriales; }
    public void setCantidadMateriales(Integer cantidadMateriales) { this.cantidadMateriales = cantidadMateriales; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public MantenimientoCinta getMantenimiento() { return mantenimiento; }
    public void setMantenimiento(MantenimientoCinta mantenimiento) { this.mantenimiento = mantenimiento; }
    public CapturaTecnica getCaptura() { return captura; }
    public void setCaptura(CapturaTecnica captura) { this.captura = captura; }
}
