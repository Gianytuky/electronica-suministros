package com.electronica.suministros.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Medición real del laboratorio para un detalle: horas capturadas, gigabytes generados y medio de entrega. */
@Entity
@Table(name = "captura_tecnica")
public class CapturaTecnica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_captura")
    private Integer idCaptura;

    @OneToOne
    @JoinColumn(name = "id_detalle")
    private DetalleOrden detalle;

    @ManyToOne
    @JoinColumn(name = "id_tecnico")
    private Usuario tecnico;

    @Column(name = "duracion_horas_reales")
    private BigDecimal duracionHorasReales;

    @Column(name = "peso_gb")
    private BigDecimal pesoGb;

    @Column(name = "medio_entrega")
    private String medioEntrega;

    @Column(name = "fecha_captura")
    private LocalDateTime fechaCaptura;

    // get y set
    public Integer getIdCaptura() { return idCaptura; }
    public void setIdCaptura(Integer idCaptura) { this.idCaptura = idCaptura; }
    public DetalleOrden getDetalle() { return detalle; }
    public void setDetalle(DetalleOrden detalle) { this.detalle = detalle; }
    public Usuario getTecnico() { return tecnico; }
    public void setTecnico(Usuario tecnico) { this.tecnico = tecnico; }
    public BigDecimal getDuracionHorasReales() { return duracionHorasReales; }
    public void setDuracionHorasReales(BigDecimal duracionHorasReales) { this.duracionHorasReales = duracionHorasReales; }
    public BigDecimal getPesoGb() { return pesoGb; }
    public void setPesoGb(BigDecimal pesoGb) { this.pesoGb = pesoGb; }
    public String getMedioEntrega() { return medioEntrega; }
    public void setMedioEntrega(String medioEntrega) { this.medioEntrega = medioEntrega; }
    public LocalDateTime getFechaCaptura() { return fechaCaptura; }
    public void setFechaCaptura(LocalDateTime fechaCaptura) { this.fechaCaptura = fechaCaptura; }
}
