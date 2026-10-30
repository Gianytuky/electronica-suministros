package com.electronica.suministros.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de Trabajo (OT): eje transaccional del sistema. Pertenece a un cliente, la registra un
 * ejecutivo y tiene muchos detalles (uno por formato recibido).
 * Estados: En Recepción -> Por Notificar -> Finalizado (o Cancelado).
 */
@Entity
@Table(name = "orden_trabajo")
public class OrdenTrabajo {

    public static final String EN_RECEPCION = "En Recepción";
    public static final String POR_NOTIFICAR = "Por Notificar";
    public static final String FINALIZADO = "Finalizado";
    public static final String CANCELADO = "Cancelado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ot")
    private Integer idOt;

    @Column(name = "codigo_ot")
    private String codigoOt;

    @ManyToOne
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "id_ejecutivo")
    private Usuario ejecutivo;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @Column(name = "estado")
    private String estado = EN_RECEPCION;

    @Column(name = "monto_estimado")
    private BigDecimal montoEstimado = BigDecimal.ZERO;

    @OneToMany(mappedBy = "ot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idDetalle ASC")
    private List<DetalleOrden> detalles = new ArrayList<>();

    /** Agrega una línea a la OT y deja enlazada la relación en ambos sentidos. */
    public void agregarDetalle(DetalleOrden detalle) {
        detalle.setOt(this);
        this.detalles.add(detalle);
    }

    /**
     * Reglas RF-05 / RF-06: la edición comercial solo es posible mientras la OT está En Recepción
     * y el técnico todavía no capturó ninguna de sus líneas (así se protege la captura técnica).
     */
    public boolean isEditable() {
        return EN_RECEPCION.equals(estado) && detalles.stream().noneMatch(d -> d.getCaptura() != null);
    }

    // get y set
    public Integer getIdOt() { return idOt; }
    public void setIdOt(Integer idOt) { this.idOt = idOt; }
    public String getCodigoOt() { return codigoOt; }
    public void setCodigoOt(String codigoOt) { this.codigoOt = codigoOt; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getEjecutivo() { return ejecutivo; }
    public void setEjecutivo(Usuario ejecutivo) { this.ejecutivo = ejecutivo; }
    public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDateTime fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getMontoEstimado() { return montoEstimado; }
    public void setMontoEstimado(BigDecimal montoEstimado) { this.montoEstimado = montoEstimado; }
    public List<DetalleOrden> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleOrden> detalles) { this.detalles = detalles; }
}
